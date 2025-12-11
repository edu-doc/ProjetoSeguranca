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
    
    // --- COMPONENTE: FIREWALL 1 (Filtro de Pacotes - Network Layer) ---
    private static Set<String> blacklist = new HashSet<>();
    
    // Configurações do IDS
    private static final String IDS_HOST = "localhost";
    private static final int IDS_PORT = 6000;

    public static void main(String[] args) throws Exception {
        // 1. Inicializa Criptografia Assimétrica (ElGamal)
        elGamalReceiver = new ImplElGamal();
        
        System.out.println("=== BORDA (DMZ) INICIADA ===");
        System.out.println("--- FIREWALL 1 (Filtro de Pacotes/Blacklist) ATIVO ---");
        System.out.println("--- PROXY FIREWALL (Inspeção de Aplicação) ATIVO ---");
        
        // Exibe chaves públicas para configurar os emissores
        System.out.println("\n--- COPIE AS CHAVES ABAIXO PARA OS EMISSORES ---");
        System.out.println("P: " + elGamalReceiver.getP());
        System.out.println("G: " + elGamalReceiver.getG());
        System.out.println("Y: " + elGamalReceiver.getY());
        System.out.println("------------------------------------------------\n");

        int porta = 55554;
        MulticastSocket ms = new MulticastSocket(porta);
        InetAddress multicastIP = InetAddress.getByName("224.0.0.1");
        InetSocketAddress grupo = new InetSocketAddress(multicastIP, porta);
        
        // Tenta pegar a interface de rede correta. Se der erro, tente pelo nome (ex: "wlan0", "eth0")
        // NetworkInterface interfaceRede = NetworkInterface.getByName("nome_da_interface"); 
        NetworkInterface interfaceRede = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        
        ms.joinGroup(grupo, interfaceRede);
        System.out.println("Receptor de Borda ouvindo em " + multicastIP.getHostAddress() + ":" + porta);

        byte[] buffer = new byte[8192]; // Buffer grande para garantir recebimento completo
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);

        while (true) {
            ms.receive(pacote);
            
            String rawData = new String(pacote.getData(), 0, pacote.getLength());
            
            // Simula identificação do IP de origem baseada no conteúdo (já que é multicast local)
            String origem = identificarOrigem(rawData); 
            
            // --- CAMADA 1: FIREWALL DE FILTRO (Bloqueio de IP/Origem) ---
            if (blacklist.contains(origem)) {
                System.out.println("🚫 [FIREWALL 1] Pacote DESCARTADO. Origem '" + origem + "' está na Blacklist.");
                continue; // Ignora o pacote e volta para o início do loop
            }

            try {
                processarPacoteSeguro(rawData, origem);
            } catch (Exception e) {
                System.err.println("Erro no processamento do pacote: " + e.getMessage());
            }
        }
    }

    private static String identificarOrigem(String rawData) {
        // Como todos enviam para o mesmo IP multicast, usamos o ID no final da string para simular o IP
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

        // --- ETAPA 1: CRIPTOGRAFIA ASSIMÉTRICA (ElGamal) ---
        // Decifra a chave de sessão que veio cifrada
        BigInteger c1 = new BigInteger(partes[0]);
        BigInteger c2 = new BigInteger(partes[1]);
        CifraElGamal cifra = new CifraElGamal(c1, c2);
        
        BigInteger chaveSimetricaBigInt = elGamalReceiver.decifrar(cifra);
        
        // --- ETAPA 2: RECUPERAR CHAVES AES E HMAC (Lógica Robusta) ---
        // Essa lógica trata o byte extra de sinal que o BigInteger pode adicionar
        byte[] chaveDecifradaCompleta = chaveSimetricaBigInt.toByteArray();

        int aesLength = 16;
        int hmacLength = 32;
        int totalKeySize = aesLength + hmacLength; // 48 bytes

        // Ajuste de array se houver bytes extras (padding de zero)
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

        // --- ETAPA 3: CRIPTOGRAFIA SIMÉTRICA (AES + HMAC) ---
        Crypto crypto = new Crypto(aesKey, hmacKeyBytes);
        
        // Decifra e valida integridade. Se HMAC falhar, lança exceção.
        String dadosAbertos = crypto.verificarEDecifrar(partes[2] + ":" + partes[3]);
        
        // --- ETAPA 4: PARSING DOS DADOS (String -> DTO) ---
        String separador = partes[4];
        String posicao = partes[5];
        
        // Converte a string "10.0-20.0-..." para objeto DroneDTO
        DroneDTO drone = parseDados(dadosAbertos, separador, posicao);
        
        if (drone == null) return; // Falha no parsing

        try {
            // --- CAMADA 2: FIREWALL PROXY (Inspeção de Conteúdo) ---
            // Verifica se os dados fazem sentido (Temperatura < 80, etc)
            FirewallProxy.inspecionarPacote(drone);
            
            // --- ETAPA 5: SUCESSO - ENVIA PARA O DATACENTER (LAN) ---
            System.out.println("Pacote de " + posicao + " verificado e seguro. Enviando para persistência...");
            new CentralService().createDrone(drone);
            
        } catch (SecurityException e) {
            // Se o Proxy pegar uma anomalia (ex: Temp 999.0 enviada pelo EmissorComprometido)
            System.err.println("🚨 ALERTA DE PROXY: " + e.getMessage());
            
            // Notifica o IDS para tomar providências (Bloquear IP)
            notificarIDS(e.getMessage(), origemSimulada);
        }
    }

    private static DroneDTO parseDados(String dados, String separador, String posicao) {
        try {
            // Usa Pattern.quote para evitar erro se o separador for caractere especial regex (ex: | ou .)
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
            
            // Envia o alerta no formato esperado pelo IDS
            out.println(mensagemErro + ":Origem:" + origem);
            
            // Lê a ordem do IDS (ex: "BLOCK:Norte" ou "ACK")
            String resposta = in.readLine();
            
            if (resposta != null && resposta.startsWith("BLOCK:")) {
                String alvo = resposta.split(":")[1];
                blacklist.add(alvo); // Adiciona na memória do Firewall 1
                System.out.println("🔥 [FIREWALL 1] BLACKLIST ATUALIZADA: " + alvo + " foi bloqueado permanentemente.");
            }
            
        } catch (Exception ex) {
            System.err.println("Falha de comunicação com o IDS: " + ex.getMessage());
            System.err.println("Certifique-se que o processo IDS.java está rodando!");
        }
    }
}