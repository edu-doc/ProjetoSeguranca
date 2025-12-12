package org.example.Conexoes.Primeiras;

import org.example.API.DTO.DroneDTO;
import org.example.Auxiliar.Cripto.Crypto;
import org.example.Auxiliar.Cripto.ImplElGamal;
import org.example.Auxiliar.Cripto.ImplElGamal.CifraElGamal;
import org.example.Domain.Service.CentralService;
import org.example.Seguranca.FirewallProxy;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.*;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ReceptorBorda {

    private static ImplElGamal elGamalReceiver;
    
    private static Set<String> blacklist = new HashSet<>();
    
    private static final String IDS_HOST = "localhost";
    private static final int IDS_PORT = 6000;

    public static void main(String[] args) throws Exception {
        elGamalReceiver = new ImplElGamal();
        
        System.out.println("=== BORDA (DMZ) INICIADA ===");
        System.out.println("--- FIREWALL 1 (Filtro de Pacotes/Blacklist) ATIVO ---");
        System.out.println("--- PROXY FIREWALL (Inspeção de Aplicação) ATIVO ---");
        
        System.out.println("\n--- COPIE AS CHAVES ABAIXO PARA OS EMISSORES ---");
        System.out.println("P: " + elGamalReceiver.getP());
        System.out.println("G: " + elGamalReceiver.getG());
        System.out.println("Y: " + elGamalReceiver.getY());
        System.out.println("------------------------------------------------\n");

        int porta = 55554;
        MulticastSocket ms = new MulticastSocket(porta);
        InetAddress multicastIP = InetAddress.getByName("224.0.0.1");
        InetSocketAddress grupo = new InetSocketAddress(multicastIP, porta);
        
        NetworkInterface interfaceRede = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        
        ms.joinGroup(grupo, interfaceRede);
        System.out.println("Receptor de Borda ouvindo em " + multicastIP.getHostAddress() + ":" + porta);

        byte[] buffer = new byte[8192];
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);

        while (true) {
            ms.receive(pacote);
            
            String rawData = new String(pacote.getData(), 0, pacote.getLength());
            
            String origem = identificarOrigem(rawData); 
            
            // --- CAMADA 1: FIREWALL DE FILTRO (Bloqueio de IP/Origem) ---
            if (blacklist.contains(origem)) {
                System.out.println("🚫 [FIREWALL 1] Pacote DESCARTADO. Origem '" + origem + "' está na Blacklist.");
                continue;
            }

            try {
                processarPacoteSeguro(rawData, origem);
            } catch (Exception e) {
                System.err.println("Erro no processamento do pacote: " + e.getMessage());
            }
        }
    }

    private static String identificarOrigem(String rawData) {
        if (rawData.contains("Norte")) return "Norte";
        if (rawData.contains("Sul")) return "Sul";
        if (rawData.contains("Leste")) return "Leste";
        if (rawData.contains("Oeste")) return "Oeste";
        return "Desconhecido";
    }

    private static void processarPacoteSeguro(String mensagem, String origemSimulada) throws Exception {
        String[] partes = mensagem.split("\\|", 6);
        if (partes.length < 6) {
            System.err.println("Pacote inválido: formato incorreto.");
            return;
        }

        BigInteger c1 = new BigInteger(partes[0]);
        BigInteger c2 = new BigInteger(partes[1]);
        CifraElGamal cifra = new CifraElGamal(c1, c2);
        
        BigInteger chaveSimetricaBigInt = elGamalReceiver.decifrar(cifra);
        
        byte[] chaveDecifradaCompleta = chaveSimetricaBigInt.toByteArray();

        int aesLength = 16;
        int hmacLength = 32;
        int totalKeySize = aesLength + hmacLength;

        if (chaveDecifradaCompleta.length != totalKeySize) {
            if (chaveDecifradaCompleta.length < totalKeySize) {
                throw new SecurityException("Chave decifrada muito curta/inválida.");
            }
            int startIndex = chaveDecifradaCompleta.length - totalKeySize;
            byte[] chaveReal = new byte[totalKeySize];
            System.arraycopy(chaveDecifradaCompleta, startIndex, chaveReal, 0, totalKeySize);
            chaveDecifradaCompleta = chaveReal;
        }

        byte[] aesKeyBytes = new byte[aesLength];
        byte[] hmacKeyBytes = new byte[hmacLength];
        System.arraycopy(chaveDecifradaCompleta, 0, aesKeyBytes, 0, aesLength);
        System.arraycopy(chaveDecifradaCompleta, aesLength, hmacKeyBytes, 0, hmacLength);

        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        Crypto crypto = new Crypto(aesKey, hmacKeyBytes);
        
        String dadosAbertos = crypto.verificarEDecifrar(partes[2] + ":" + partes[3]);
        
        String separador = partes[4];
        String posicao = partes[5];
        
        DroneDTO drone = parseDados(dadosAbertos, separador, posicao);
        
        if (drone == null) return;

        try {
            // --- CAMADA 2: FIREWALL PROXY (Inspeção de Conteúdo) ---
            FirewallProxy.inspecionarPacote(drone);
            
            System.out.println("Pacote de " + posicao + " verificado e seguro. Enviando para persistência...");
            new CentralService().createDrone(drone);
            
        } catch (SecurityException e) {
            System.err.println("🚨 ALERTA DE PROXY: " + e.getMessage());
            
            // Notifica o IDS para tomar providências (Bloquear IP)
            notificarIDS(e.getMessage(), origemSimulada);
        }
    }

    private static DroneDTO parseDados(String dados, String separador, String posicao) {
        try {
            String[] v = dados.split(Pattern.quote(separador));
            
            if (v.length < 12) {
                System.err.println("Erro de parsing: dados insuficientes.");
                return null;
            }

            return new DroneDTO(
                Double.parseDouble(v[0]), Double.parseDouble(v[1]), Double.parseDouble(v[2]), Double.parseDouble(v[3]), // CO2, CO, NO2, SO2
                Double.parseDouble(v[4]), Double.parseDouble(v[5]), // PM2.5, PM10
                Double.parseDouble(v[6]), Double.parseDouble(v[7]), // Temp, Umid
                Double.parseDouble(v[8]), Double.parseDouble(v[9]), // Ruido, Rad
                Double.parseDouble(v[10]), Double.parseDouble(v[11]), // Lat, Long
                posicao
            );
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter números do pacote: " + e.getMessage());
            return null;
        }
    }

    private static void notificarIDS(String mensagemErro, String origem) {
        System.out.println("📡 Contactando IDS para reportar incidente em " + origem + "...");
        try (Socket socket = new Socket(IDS_HOST, IDS_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println(mensagemErro + ":Origem:" + origem);
            
            String resposta = in.readLine();
            
            if (resposta != null && resposta.startsWith("BLOCK:")) {
                String alvo = resposta.split(":")[1];
                blacklist.add(alvo);
                System.out.println("🔥 [FIREWALL 1] BLACKLIST ATUALIZADA: " + alvo + " foi bloqueado permanentemente.");
            }
            
        } catch (Exception ex) {
            System.err.println("Falha de comunicação com o IDS: " + ex.getMessage());
            System.err.println("Certifique-se que o processo IDS.java está rodando!");
        }
    }
}