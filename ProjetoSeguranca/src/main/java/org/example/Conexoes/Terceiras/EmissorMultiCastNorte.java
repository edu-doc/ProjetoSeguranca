package org.example.Conexoes.Terceiras;

import org.example.Auxiliar.Cripto.ImplElGamal;
import org.example.Auxiliar.Cripto.ImplElGamal.CifraElGamal;
import org.example.Auxiliar.Cripto.Crypto;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Random;

public class EmissorMultiCastNorte {

    // ⚠️ ATENÇÃO: ESTAS DEVEM SER AS CHAVES PÚBLICAS REAIS (P, G, Y) GERADAS PELO ReceptorMultiCast.java
    // Você deve copiar os valores do console do Receptor após iniciá-lo.
    // Usei BigIntegers grandes para simular a segurança.
    private static final BigInteger P_RECEPTOR = new BigInteger("17142135118797968536098059040003714213511879796853609805904000371421351187979685360980590400037");
    private static final BigInteger G_RECEPTOR = BigInteger.valueOf(2);
    private static final BigInteger Y_RECEPTOR = new BigInteger("891011121314151617181920212223242526272829303132333435363738394041424344454647484950515253545556575859");

    private static final String POSICAO = "Norte";
    private static final String SEPARADOR = "-";
    private static final int DELAY_MS = 2500; // Ajustado para a faixa (2-3 segundos)

    public static void main(String[] args) {
        String apiUrl = "http://localhost:8081/api/sensores/dados/" + POSICAO;
        String multicastIp = "224.0.0.1";
        int porta = 55554;

        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(multicastIp);
            socket.setTimeToLive(1);

            // Inicializa ElGamal com a chave pública do Receptor
            ImplElGamal elGamalSender = new ImplElGamal(P_RECEPTOR, G_RECEPTOR, Y_RECEPTOR);

            while (true) {
                // 1. Coleta os dados (String de 12 campos)
                String dadosAbertos = buscarDadosDaAPI(apiUrl);

                // 2. Gerar chaves de sessão AES e HMAC aleatórias (novas a cada mensagem)
                SecretKeySpec aesKey = gerarAESKey();
                byte[] hmacKey = gerarHmacKey();

                // 3. Empacota a chave de sessão AES + HMAC em um BigInteger
                BigInteger chaveSimetricaBigInt = empacotarChaveSimetrica(aesKey.getEncoded(), hmacKey);

                // 4. Cifra a chave de sessão usando ElGamal
                CifraElGamal cifraElGamal = elGamalSender.cifrar(chaveSimetricaBigInt);

                // 5. Cifra e Autentica os dados (AES + HMAC)
                Crypto crypto = new Crypto(aesKey, hmacKey);
                String dadosCifradosEAutenticados = crypto.cifrarEAutenticar(dadosAbertos); // Formato HMAC:CIPHERTEXT

                // 6. Constrói a mensagem final (c1|c2|HMAC|CIPHERTEXT|SEPARADOR|POSICAO)
                String[] partesCripto = dadosCifradosEAutenticados.split(":", 2);

                String mensagemCompleta =
                        cifraElGamal.getC1().toString() + "|" +
                                cifraElGamal.getC2().toString() + "|" +
                                partesCripto[0] + "|" + // HMAC em Base64
                                partesCripto[1] + "|" + // Ciphertext em Base64
                                SEPARADOR + "|" +
                                POSICAO;

                byte[] buffer = mensagemCompleta.getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, porta);
                socket.send(packet);

                Thread.sleep(DELAY_MS);
            }

        } catch (IOException e) {
            System.err.println("EMISSOR ("+POSICAO+"): Erro de E/S: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("EMISSOR ("+POSICAO+"): Erro de Criptografia ou Comutação: " + e.getMessage());
        }
    }

    private static String buscarDadosDaAPI(String urlString) {
        StringBuilder resposta = new StringBuilder();

        try {
            URL url = new URL(urlString);
            HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
            conexao.setRequestMethod("GET");

            if (conexao.getResponseCode() == 200) {
                try (BufferedReader leitor = new BufferedReader(new InputStreamReader(conexao.getInputStream()))) {
                    String linha;
                    while ((linha = leitor.readLine()) != null) {
                        resposta.append(linha);
                    }
                }
            } else {
                resposta.append("0-0-0-0-0-0-0-0-0-0-0-0");
            }

            conexao.disconnect();
        } catch (IOException e) {
            resposta.append("0-0-0-0-0-0-0-0-0-0-0-0");
        }

        return resposta.toString();
    }

    // --- UTILS PARA CHAVES DE SESSÃO ---
    private static SecretKeySpec gerarAESKey() {
        byte[] keyBytes = new byte[16]; // AES-128 (16 bytes)
        new SecureRandom().nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, "AES");
    }

    private static byte[] gerarHmacKey() {
        byte[] keyBytes = new byte[32]; // HMAC-SHA256 (32 bytes - 256 bits)
        new SecureRandom().nextBytes(keyBytes);
        return keyBytes;
    }

    // Converte (AES Key + HMAC Key) em um BigInteger para cifrar com ElGamal
    private static BigInteger empacotarChaveSimetrica(byte[] aesKeyBytes, byte[] hmacKeyBytes) {
        byte[] combinado = new byte[aesKeyBytes.length + hmacKeyBytes.length];
        System.arraycopy(aesKeyBytes, 0, combinado, 0, aesKeyBytes.length);
        System.arraycopy(hmacKeyBytes, 0, combinado, aesKeyBytes.length, hmacKeyBytes.length);

        return new BigInteger(1, combinado);
    }
}