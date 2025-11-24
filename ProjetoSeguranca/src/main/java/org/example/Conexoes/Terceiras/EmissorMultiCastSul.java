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

public class EmissorMultiCastSul {

    private static final String POSICAO = "Sul";
    private static final String SEPARADOR = ";"; // Separador para Sul
    private static final int DELAY_MS = 2500;

    public static void main(String[] args) {

        if (args.length < 3) {
            System.err.println("Uso: java EmissorMultiCastSul <P_RECEPTOR> <G_RECEPTOR> <Y_RECEPTOR>");
            return;
        }

        // Injeção de Chaves via Argumentos
        BigInteger P_RECEPTOR = new BigInteger(args[0]);
        BigInteger G_RECEPTOR = new BigInteger(args[1]);
        BigInteger Y_RECEPTOR = new BigInteger(args[2]);

        String apiUrl = "http://localhost:8081/api/sensores/dados/" + POSICAO;
        String multicastIp = "224.0.0.1";
        int porta = 55554;

        try (MulticastSocket socket = new MulticastSocket()) {
            InetAddress group = InetAddress.getByName(multicastIp);
            socket.setTimeToLive(1);

            ImplElGamal elGamalSender = new ImplElGamal(P_RECEPTOR, G_RECEPTOR, Y_RECEPTOR);

            while (true) {
                String dadosAbertos = buscarDadosDaAPI(apiUrl, SEPARADOR);

                SecretKeySpec aesKey = gerarAESKey();
                byte[] hmacKey = gerarHmacKey();

                BigInteger chaveSimetricaBigInt = empacotarChaveSimetrica(aesKey.getEncoded(), hmacKey);
                CifraElGamal cifraElGamal = elGamalSender.cifrar(chaveSimetricaBigInt);

                Crypto crypto = new Crypto(aesKey, hmacKey);
                String dadosCifradosEAutenticados = crypto.cifrarEAutenticar(dadosAbertos);

                String[] partesCripto = dadosCifradosEAutenticados.split(":", 2);

                String mensagemCompleta =
                        cifraElGamal.getC1().toString() + "|" +
                                cifraElGamal.getC2().toString() + "|" +
                                partesCripto[0] + "|" +
                                partesCripto[1] + "|" +
                                SEPARADOR + "|" +
                                POSICAO;

                byte[] buffer = mensagemCompleta.getBytes();

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, porta);
                socket.send(packet);
                System.out.println("EMISSOR ("+POSICAO+"): Pacote enviado com chave de sessão ElGamal cifrada.");

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

    private static String buscarDadosDaAPI(String urlString, String separador) {
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
                resposta.append("0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0");
            }

            conexao.disconnect();
        } catch (IOException e) {
            resposta.append("0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0" + separador + "0");
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

    private static BigInteger empacotarChaveSimetrica(byte[] aesKeyBytes, byte[] hmacKeyBytes) {
        byte[] combinado = new byte[aesKeyBytes.length + hmacKeyBytes.length];
        System.arraycopy(aesKeyBytes, 0, combinado, 0, aesKeyBytes.length);
        System.arraycopy(hmacKeyBytes, 0, combinado, aesKeyBytes.length, hmacKeyBytes.length);

        return new BigInteger(1, combinado);
    }
}