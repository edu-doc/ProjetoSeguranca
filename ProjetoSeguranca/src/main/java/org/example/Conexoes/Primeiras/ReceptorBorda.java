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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReceptorBorda {

    private static ImplElGamal elGamalReceiver;
    // Firewall 1: Lista Negra (Blacklist) em memória
    private static Set<String> blacklist = new HashSet<>();
    private static final String IDS_HOST = "localhost";
    private static final int IDS_PORT = 6000;

    public static void main(String[] args) throws Exception {
        // Inicializa Criptografia
        elGamalReceiver = new ImplElGamal();
        System.out.println("=== BORDA (DMZ) INICIADA ===");
        System.out.println("--- FIREWALL 1 (Filtro de Pacotes) ATIVO ---");
        System.out.println("P: " + elGamalReceiver.getP());
        System.out.println("G: " + elGamalReceiver.getG());
        System.out.println("Y: " + elGamalReceiver.getY());

        int porta = 55554;
        MulticastSocket ms = new MulticastSocket(porta);
        InetAddress multicastIP = InetAddress.getByName("224.0.0.1");
        InetSocketAddress grupo = new InetSocketAddress(multicastIP, porta);
        
        // Ajuste a interface conforme necessário
        NetworkInterface interfaceRede = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()); 
        // Se der erro, tente: NetworkInterface.getByName("nome_interface");
        
        ms.joinGroup(grupo, interfaceRede);

        byte[] buffer = new byte[8192];
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);

        while (true) {
            ms.receive(pacote);
            
            // --- CAMADA 1: FIREWALL DE FILTRO DE PACOTES (Simulado) ---
            // Como é multicast local, simulamos o bloqueio baseado no ID/Posição que vem no pacote 
            // (na prática real, bloquearíamos pelo IP: pacote.getAddress())
            
            String rawData = new String(pacote.getData(), 0, pacote.getLength());
            
            // Pré-processamento leve para identificar origem (simulando header IP)
            String origem = identificarOrigem(rawData); 
            
            if (blacklist.contains(origem)) {
                System.out.println("🚫 [FIREWALL 1] Pacote DESCARTADO. Origem " + origem + " está na Blacklist.");
                continue; // Descarta o pacote
            }

            try {
                processarPacoteSeguro(rawData, origem);
            } catch (Exception e) {
                System.err.println("Erro no processamento: " + e.getMessage());
            }
        }
    }

    private static String identificarOrigem(String rawData) {
        // Uma verificação rápida no final da string para simular o IP de origem
        if (rawData.contains("Norte")) return "Norte";
        if (rawData.contains("Sul")) return "Sul";
        if (rawData.contains("Leste")) return "Leste";
        if (rawData.contains("Oeste")) return "Oeste";
        return "Desconhecido";
    }

    private static void processarPacoteSeguro(String mensagem, String origemSimulada) throws Exception {
        // --- ETAPA DE DECIFRAGEM (Receptor) ---
        // (Código reutilizado e adaptado do ReceptorMultiCast original)
        String[] partes = mensagem.split("\\|", 6);
        if (partes.length < 6) return;

        BigInteger c1 = new BigInteger(partes[0]);
        BigInteger c2 = new BigInteger(partes[1]);
        CifraElGamal cifra = new CifraElGamal(c1, c2);
        
        BigInteger chaveSimetrica = elGamalReceiver.decifrar(cifra);
        
        // Extração das chaves AES e HMAC (Simplificada para brevidade, usar lógica robusta do original)
        byte[] chaveBytes = chaveSimetrica.toByteArray();
        // ... (Assumindo extração correta dos 48 bytes como no original) ...
        // Para garantir funcionamento rápido no exemplo, vamos recalcular o split se necessário
        // (Vide código original para lógica de padding de bytes)
        
        // --- SIMPLIFICAÇÃO PARA O EXEMPLO (Assumindo que funcionou a decifragem da chave) ---
        // No código real, copie a lógica de "Extração Robusta" do ReceptorMultiCast aqui.
        
        // Vamos focar na integração com o Firewall 2 e IDS
        
        // Suponha que deciframos e temos os dados abertos:
        // String dadosAbertos = crypto.verificarEDecifrar(...);
        // Para este exemplo, vamos simular que deciframos com sucesso para mostrar o IDS
        // (Você deve manter a lógica de criptografia real do seu arquivo anterior aqui)
        
        // Simulando a obtenção do DTO após decifrar
        // DroneDTO drone = converterStringParaDTO(dadosAbertos, ...);
        
        // --- INTEGRAÇÃO COM FIREWALL PROXY & IDS ---
        // Aqui é onde inserimos a lógica nova solicitada na prática
        
        // Vamos supor que recuperamos o objeto DroneDTO após toda a criptografia
        // Vou criar um objeto fictício aqui para ilustrar a lógica do Proxy,
        // mas você usará os dados reais decifrados.
        
        // ** PONTO CRÍTICO: Se o EMISSOR for o comprometido, ele enviará dados ruins **
        // A decifragem funcionará (pois as chaves estão certas), mas o CONTEÚDO será malicioso.
        
        // Exemplo de integração real (pseudo-código sobre o fluxo):
        // 1. Decifra
        // 2. Cria DTO
        // 3. Chama Proxy
        
        // REINSERINDO LÓGICA DE DECIFRAÇÃO (Resumida para caber na resposta)
        byte[] chaveDecifradaCompleta = chaveSimetrica.toByteArray();
        if (chaveDecifradaCompleta.length > 48) { 
             byte[] temp = new byte[48];
             System.arraycopy(chaveDecifradaCompleta, chaveDecifradaCompleta.length - 48, temp, 0, 48);
             chaveDecifradaCompleta = temp;
        }
        byte[] aesKeyBytes = new byte[16];
        byte[] hmacKeyBytes = new byte[32];
        System.arraycopy(chaveDecifradaCompleta, 0, aesKeyBytes, 0, 16);
        System.arraycopy(chaveDecifradaCompleta, 16, hmacKeyBytes, 0, 32);
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        Crypto crypto = new Crypto(aesKey, hmacKeyBytes);
        
        String dadosAbertos = crypto.verificarEDecifrar(partes[2] + ":" + partes[3]);
        DroneDTO drone = parseDados(dadosAbertos, partes[4], partes[5]);
        
        try {
            // --- CAMADA 2: FIREWALL PROXY (Aplicação) ---
            FirewallProxy.inspecionarPacote(drone);
            
            // Se passou, envia para a LAN (Banco de Dados)
            new CentralService().createDrone(drone);
            
        } catch (SecurityException e) {
            // Anomalia detectada pelo Proxy!
            // Reportar ao IDS
            notificarIDS(e.getMessage(), origemSimulada);
        }
    }

    private static void notificarIDS(String mensagemErro, String origem) {
        try (Socket socket = new Socket(IDS_HOST, IDS_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            // Envia alerta formatado
            out.println(mensagemErro + ":Origem:" + origem);
            
            // Lê resposta do IDS
            String resposta = in.readLine();
            if (resposta != null && resposta.startsWith("BLOCK:")) {
                String alvo = resposta.split(":")[1];
                blacklist.add(alvo); // Atualiza Firewall 1
                System.out.println("🔥 [FIREWALL 1] Regra atualizada: " + alvo + " adicionado à BLACKLIST.");
            }
            
        } catch (Exception ex) {
            System.err.println("Falha ao contatar IDS: " + ex.getMessage());
        }
    }

    private static DroneDTO parseDados(String dados, String separador, String posicao) {
        // Lógica de parsing igual ao ReceptorMultiCast original
        // ...
        // (Simplificado para o exemplo, retorne o DTO preenchido)
        String[] v = dados.split(java.util.regex.Pattern.quote(separador));
        // Assumindo que v tem 12 posições corretas
        return new DroneDTO(
            Double.parseDouble(v[0]), Double.parseDouble(v[1]), Double.parseDouble(v[2]), Double.parseDouble(v[3]),
            Double.parseDouble(v[4]), Double.parseDouble(v[5]), Double.parseDouble(v[6]), Double.parseDouble(v[7]),
            Double.parseDouble(v[8]), Double.parseDouble(v[9]), Double.parseDouble(v[10]), Double.parseDouble(v[11]),
            posicao
        );
    }
}