package org.example.Auxiliar.Cripto;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Factory.Argon2Types;

// Implementação do Argon2id (vencedor do Password Hashing Competition)
public class ImplArgon2 {

    // Configuração do Argon2id: 32 bytes de salt, 64 bytes de hash
    private static final Argon2 argon2 = Argon2Factory.create(Argon2Types.ARGON2id, 32, 64);

    // Parâmetros de custo (t=iterações, m=memória, p=paralelismo)
    private static final int ITERATIONS = 4;
    private static final int MEMORY = 65536; // 64 MiB
    private static final int PARALLELISM = 2;

    private ImplArgon2() {}

    /**
     * Gera o hash de uma senha usando Argon2id.
     * @param senha A senha em texto claro.
     * @return O hash da senha no formato Argon2 (incluindo salt e parâmetros).
     */
    public static String gerarHash(char[] senha) {
        String hash = null;
        try {
            // Gera o hash com os parâmetros definidos
            hash = argon2.hash(ITERATIONS, MEMORY, PARALLELISM, senha);
            return hash;
        } finally {
            // Apaga a senha da memória (boa prática de segurança)
            argon2.wipeArray(senha);
        }
    }

    /**
     * Verifica se uma senha corresponde a um hash Argon2.
     * @param hash O hash Argon2 armazenado.
     * @param senha A senha em texto claro.
     * @return true se a senha for válida, false caso contrário.
     */
    public static boolean verificar(String hash, char[] senha) {
        try {
            return argon2.verify(hash, senha);
        } finally {
            // Apaga a senha da memória.
            argon2.wipeArray(senha);
        }
    }

    // Método utilitário para limpar arrays de senha
    public static void limparArray(char[] array) {
        argon2.wipeArray(array);
    }
}