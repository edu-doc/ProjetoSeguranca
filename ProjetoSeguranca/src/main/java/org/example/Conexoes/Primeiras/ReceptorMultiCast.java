package org.example.Conexoes.Primeiras;

import org.example.API.DTO.DroneDTO;
import org.example.Domain.Service.CentralService;
import org.example.Auxiliar.Cripto.ImplElGamal;
import org.example.Auxiliar.Cripto.ImplElGamal.CifraElGamal;
import org.example.Auxiliar.Cripto.Crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class ReceptorMultiCast {

    private static ImplElGamal elGamalReceiver;

    public static void main(String[] args) throws IOException {

        // 1. INICIALIZAÇÃO ELGAMAL (Gera o par de chaves Privada/Pública)
        elGamalReceiver = new ImplElGamal();
        System.out.println("--- Borda (Receptor) Inicializado ---");

        // Versão completa das chaves para fácil cópia (REQUER COPIAR ESSES VALORES PARA OS 4 EMISSORES)
        System.out.println("--- CHAVES PÚBLICAS (COPIAR PARA EMISSORES) ---");
        System.out.println("P_RECEPTOR: " + elGamalReceiver.getP().toString());
        System.out.println("G_RECEPTOR: " + elGamalReceiver.getG().toString());
        System.out.println("Y_RECEPTOR: " + elGamalReceiver.getY().toString());
        System.out.println("----------------------------------------------");

        int porta = 55554;
        String mensagem = "";

        MulticastSocket ms = new MulticastSocket(porta);
        InetAddress multicastIP = InetAddress.getByName("224.0.0.1");
        InetSocketAddress grupo = new InetSocketAddress(multicastIP, porta);
        // **ATENÇÃO:** Mantenha ou corrija para a sua interface de rede local (ex: "ethernet_32768")
        NetworkInterface interfaceRede = NetworkInterface.getByName("wireless_32768");

        ms.joinGroup(grupo, interfaceRede);

        System.out.println("Receptor contínuo ouvindo em " + multicastIP.getHostAddress() + ":" + porta);

        byte[] buffer = new byte[4096];
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                ms.receive(pacote);
                mensagem = new String(pacote.getData(), 0, pacote.getLength());
                formatarMensagem(mensagem);
            } catch (IOException e) {
                System.err.println("Erro ao receber pacote: " + e.getMessage());
            }

        }
    }

    static CentralService centralService = new CentralService();
    static List<Double> numeros;

    public static void formatarMensagem(String mensagem) {
        String[] partesPrincipais = mensagem.split("\\|", 6);

        if (partesPrincipais.length != 6) {
            System.err.println("Erro: Formato de mensagem insegura inválido. Recebido " + partesPrincipais.length + " partes. Mensagem: " + mensagem);
            return;
        }

        try {
            // --- 1. DECIFRAR CHAVE DE SESSÃO COM ELGAMAL ---
            BigInteger c1 = new BigInteger(partesPrincipais[0]);
            BigInteger c2 = new BigInteger(partesPrincipais[1]);

            CifraElGamal cifraElGamal = new CifraElGamal(c1, c2);
            BigInteger chaveSimetricaBigInt = elGamalReceiver.decifrar(cifraElGamal);

            // --- 2. RECUPERAR CHAVES AES E HMAC (Extração Robusta) ---
            byte[] chaveDecifradaCompleta = chaveSimetricaBigInt.toByteArray();

            int aesLength = 16;
            int hmacLength = 32;
            int totalKeySize = aesLength + hmacLength; // 48 bytes esperados

            // Se o tamanho não for o esperado (provavelmente 64 bytes devido ao padding do BigInteger)
            if (chaveDecifradaCompleta.length != totalKeySize) {

                if (chaveDecifradaCompleta.length < totalKeySize) {
                    throw new SecurityException("Chave decifrada muito curta. Tam: " + chaveDecifradaCompleta.length);
                }

                // Calcula o índice de onde a chave real de 48 bytes começa (ignorando os zeros/padding no início)
                int startIndex = chaveDecifradaCompleta.length - totalKeySize;

                // Copia os 48 bytes de chave reais (os últimos 48 bytes)
                byte[] chaveReal = new byte[totalKeySize];
                System.arraycopy(chaveDecifradaCompleta, startIndex, chaveReal, 0, totalKeySize);
                chaveDecifradaCompleta = chaveReal;
            }

            // Verificação final (deve ser 48 bytes)
            if (chaveDecifradaCompleta.length != totalKeySize) {
                throw new SecurityException("Chave simétrica decifrada com tamanho inválido após ajuste. Tam: " + chaveDecifradaCompleta.length + ". Esperado: " + totalKeySize);
            }

            // Separação das chaves
            byte[] aesKeyBytes = new byte[aesLength];
            System.arraycopy(chaveDecifradaCompleta, 0, aesKeyBytes, 0, aesLength);

            byte[] hmacKeyBytes = new byte[hmacLength];
            System.arraycopy(chaveDecifradaCompleta, aesLength, hmacKeyBytes, 0, hmacLength);

            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // --- 3. DECIFRAR E VERIFICAR DADOS COM AES/HMAC ---
            String hmacBase64 = partesPrincipais[2];
            String ciphertextBase64 = partesPrincipais[3];

            String mensagemCifradaHmac = hmacBase64 + ":" + ciphertextBase64;

            Crypto crypto = new Crypto(aesKey, hmacKeyBytes);
            String dadosAbertos = crypto.verificarEDecifrar(mensagemCifradaHmac);

            // --- 4. PROCESSAR DADOS DE DRONE ---
            String separador = partesPrincipais[4];
            String posicao = partesPrincipais[5];

            processarDadosDecifrados(dadosAbertos, separador, posicao);

        } catch (SecurityException e) {
            System.err.println("--- ALERTA DE SEGURANÇA ---");
            System.err.println("Rejeitado: Falha na verificação de HMAC (Integridade/Autenticidade). Pacote adulterado/inválido: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro de Criptografia/Decifragem: " + e.getMessage() + ". Ignorando pacote. ");
        }
    }

    private static void processarDadosDecifrados(String mensagem, String separador, String posicao) {
        String[] partes = mensagem.split(java.util.regex.Pattern.quote(separador));

        if (partes.length != 12) {
            System.err.println("Erro: Número incorreto de campos de dados após decifragem. Esperado 12, recebido " + partes.length + ".");
            return;
        }

        numeros = new ArrayList<>();

        // Recuperação dos 12 números
        for (String parte : partes) {
            try {
                numeros.add(Double.parseDouble(parte.trim()));
            } catch (NumberFormatException e) {
                System.err.println("Valor numérico inválido após decifragem: '" + parte + "'. ");
                return;
            }
        }

        // --- LÓGICA DE ALERTA RÁPIDO NA BORDA (EDGE COMPUTING) ---
        // Ordem: 0: co2, 4: pm2_5, 8: ruido
        if (numeros.get(0) > 1000) { // Limite CO2 (Alto: > 1000 ppm)
            System.err.println("🚨 ALERTA BORDA (CO2): Nível elevado! " + String.format("%.2f ppm", numeros.get(0)) + " em " + posicao + ".");
        }

        if (numeros.get(4) > 35) { // Limite PM2.5 (Qualidade do ar ruim)
            System.err.println("🚨 ALERTA BORDA (PM2.5): Qualidade do ar comprometida! " + String.format("%.1f µg/m³", numeros.get(4)) + " em " + posicao + ".");
        }

        if (numeros.get(8) > 85) { // Limite Ruído (Alto: > 85 dB)
            System.err.println("🚨 ALERTA BORDA (RUÍDO): Ruído excessivo detectado: " + String.format("%.1f dB", numeros.get(8)) + " em " + posicao + ".");
        }
        // -----------------------------------------------------------------

        // Cria o DTO com os 12 campos + Posição
        DroneDTO droneDTO = new DroneDTO(
                numeros.get(0), numeros.get(1), numeros.get(2), numeros.get(3),
                numeros.get(4), numeros.get(5),
                numeros.get(6), numeros.get(7),
                numeros.get(8), numeros.get(9),
                numeros.get(10), numeros.get(11),
                posicao
        );

        centralService.createDrone(droneDTO);
        System.out.println("Mensagem do drone do " + posicao + " recebida, decifrada, verificada e processada.");
        numeros.clear();
    }
}