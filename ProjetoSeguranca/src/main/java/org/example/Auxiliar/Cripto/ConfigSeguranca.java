package org.example.Auxiliar.Cripto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class ConfigSeguranca {

    public static final String SEGREDO_HMAC = "supoer-chave-secreta";

    private static final String SEGREDO_AES_STRING = "x84kd213fxz469bn";
    public static final SecretKey CHAVE_AES;
    public static final byte[] CHAVE_HMAC_BYTES;

    static {
        byte[] aesKeyBytes = SEGREDO_AES_STRING.getBytes(StandardCharsets.UTF_8);
        CHAVE_AES = new SecretKeySpec(aesKeyBytes, 0, aesKeyBytes.length, "AES");

        CHAVE_HMAC_BYTES = SEGREDO_HMAC.getBytes(StandardCharsets.UTF_8);
    }

}