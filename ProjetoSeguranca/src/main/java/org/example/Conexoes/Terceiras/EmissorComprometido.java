package org.example.Conexoes.Terceiras;

import org.example.Auxiliar.Cripto.ImplElGamal;
import org.example.Auxiliar.Cripto.ImplElGamal.CifraElGamal;
import org.example.Auxiliar.Cripto.Crypto;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.net.*;

public class EmissorComprometido {
    // Mesma estrutura dos outros, mas dados hardcoded maliciosos
    private static final String POSICAO = "Norte"; // Dispositivo do Norte foi comprometido
    private static final String SEPARADOR = "-";

    public static void main(String[] args) {
        if (args.length < 3) return;
        BigInteger P = new BigInteger(args[0]);
        BigInteger G = new BigInteger(args[1]);
        BigInteger Y = new BigInteger(args[2]);

        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName("224.0.0.1");
            ImplElGamal elGamal = new ImplElGamal(P, G, Y);
            
            System.out.println("😈 [EMISSOR COMPROMETIDO] Iniciando ataque de injeção de dados...");

            while (true) {
                // GERA DADOS MALICIOSOS (Temperatura 999.0)
                String dadosMaliciosos = "5000-50-50-50-50-50-999.0-50-200.0-50-0-0";
                
                // Processo normal de criptografia (para passar pela autenticação, mas falhar no conteúdo)
                SecretKeySpec aesKey = gerarAESKey();
                byte[] hmacKey = gerarHmacKey();
                BigInteger keyPack = empacotarChaveSimetrica(aesKey.getEncoded(), hmacKey);
                CifraElGamal cifra = elGamal.cifrar(keyPack);
                Crypto crypto = new Crypto(aesKey, hmacKey);
                String payload = crypto.cifrarEAutenticar(dadosMaliciosos);
                String[] parts = payload.split(":", 2);

                String msg = cifra.getC1() + "|" + cifra.getC2() + "|" + parts[0] + "|" + parts[1] + "|" + SEPARADOR + "|" + POSICAO;
                
                byte[] b = msg.getBytes();
                socket.send(new DatagramPacket(b, b.length, group, 55554));
                System.out.println("😈 Dados anômalos enviados.");
                
                Thread.sleep(2000);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    // (Copiar métodos auxiliares gerarAESKey, etc. dos outros emissores)
    private static SecretKeySpec gerarAESKey() {
        byte[] keyBytes = new byte[16]; new SecureRandom().nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, "AES");
    }
    private static byte[] gerarHmacKey() {
        byte[] keyBytes = new byte[32]; new SecureRandom().nextBytes(keyBytes);
        return keyBytes;
    }
    private static BigInteger empacotarChaveSimetrica(byte[] aes, byte[] hmac) {
        byte[] comb = new byte[aes.length + hmac.length];
        System.arraycopy(aes, 0, comb, 0, aes.length);
        System.arraycopy(hmac, 0, comb, aes.length, hmac.length);
        return new BigInteger(1, comb);
    }
}