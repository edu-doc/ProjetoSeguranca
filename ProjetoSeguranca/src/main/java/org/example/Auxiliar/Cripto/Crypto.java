package org.example.Auxiliar.Cripto;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Crypto {

    private ImplAES implAES;
    private byte[] chaveHmac;

    public Crypto(SecretKey chaveAes, byte[] chaveHmac) {
        this.implAES = new ImplAES(chaveAes);
        this.chaveHmac = chaveHmac;
    }

    public String cifrarEAutenticar(String textoAberto) throws Exception {

        String ciphertextBase64 = implAES.cifrar(textoAberto); //

        byte[] ciphertextBytes = ciphertextBase64.getBytes(StandardCharsets.UTF_8);
        byte[] hmacBytes = ImplHMacSha256.calcularHmacSha256(this.chaveHmac, ciphertextBytes); //

        String hmacBase64 = Base64.getEncoder().encodeToString(hmacBytes);

        return hmacBase64 + ":" + ciphertextBase64;
    }

    public String verificarEDecifrar(String mensagemCombinada) throws Exception {

        String[] parts = mensagemCombinada.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato da mensagem inválido. Esperado 'hmac:ciphertext'.");
        }

        String hmacBase64 = parts[0];
        String ciphertextBase64 = parts[1];

        byte[] hmacRecebido = Base64.getDecoder().decode(hmacBase64);
        byte[] ciphertextBytes = ciphertextBase64.getBytes(StandardCharsets.UTF_8);

        boolean hmacValido = ImplHMacSha256.checarHmac(this.chaveHmac, ciphertextBytes, hmacRecebido); //

        if (!hmacValido) {
            throw new SecurityException("Falha na verificação do HMAC. Mensagem inválida ou adulterada.");
        }

        return implAES.decifrar(ciphertextBase64); //
    }

}