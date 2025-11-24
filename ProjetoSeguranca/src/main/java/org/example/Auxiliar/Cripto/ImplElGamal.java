package org.example.Auxiliar.Cripto;

import java.math.BigInteger;
import java.security.SecureRandom;

public class ImplElGamal {

    // Tamanho do primo p (em bits).
    // Usar um tamanho maior em produção (ex: 1024 ou 2048)
    private static final int BIT_LENGTH = 512;
    private static final SecureRandom RANDOM = new SecureRandom();

    private BigInteger p; // Primo grande
    private BigInteger g; // Gerador
    private BigInteger x; // Chave Privada
    private BigInteger y; // Chave Pública

    public ImplElGamal() {
        // 1. Escolher um primo grande p
        this.p = BigInteger.probablePrime(BIT_LENGTH, RANDOM);
        // 2. Escolher um número gerador g (simplificado: p-1)
        this.g = BigInteger.valueOf(2);
        // 3. Escolher um número aleatório x (Chave Privada)
        // tal que 1 < x < p-1
        this.x = new BigInteger(p.bitLength(), RANDOM).mod(p.subtract(BigInteger.ONE)).add(BigInteger.ONE);
        // 4. Calcular y = g^x mod p (Chave Pública)
        this.y = g.modPow(x, p);
    }

    // Construtor para receptor que conhece o par de chaves
    public ImplElGamal(BigInteger p, BigInteger g, BigInteger y, BigInteger x) {
        this.p = p;
        this.g = g;
        this.y = y;
        this.x = x;
    }

    // Construtor para emissor que só conhece a chave pública do receptor
    public ImplElGamal(BigInteger p, BigInteger g, BigInteger y) {
        this.p = p;
        this.g = g;
        this.y = y;
        this.x = null; // Emissor não tem a chave privada
    }

    // --- MÉTODOS DE CHAVE ---

    public BigInteger getP() { return p; }
    public BigInteger getG() { return g; }
    public BigInteger getY() { return y; }
    public BigInteger getX() { return x; }

    // --- CIFRAGEM (Emissor) ---
    // Recebe a chave de sessão (AES Key) como um BigInteger 'm' e a cifra
    public CifraElGamal cifrar(BigInteger m) {
        if (y == null) {
            throw new IllegalStateException("Chave pública (y) não definida para cifragem.");
        }

        // 1. Escolher aleatoriamente um número k, tal que 1 < k < p-1 e mdc(k, p-1) = 1
        BigInteger k;
        BigInteger p_minus_1 = p.subtract(BigInteger.ONE);
        do {
            k = new BigInteger(p.bitLength(), RANDOM).mod(p_minus_1).add(BigInteger.ONE);
        } while (!k.gcd(p_minus_1).equals(BigInteger.ONE));

        // 2. Calcular c1 = g^k mod p
        BigInteger c1 = g.modPow(k, p);

        // 3. Calcular c2 = (m * y^k) mod p
        BigInteger y_pow_k = y.modPow(k, p);
        BigInteger c2 = m.multiply(y_pow_k).mod(p);

        return new CifraElGamal(c1, c2);
    }

    // --- DECIFRAGEM (Receptor) ---
    // Decifra o par (c1, c2) usando a chave privada 'x' para obter a chave de sessão 'm'
    public BigInteger decifrar(CifraElGamal cifra) {
        if (x == null) {
            throw new IllegalStateException("Chave privada (x) não definida para decifragem.");
        }

        BigInteger c1 = cifra.getC1();
        BigInteger c2 = cifra.getC2();

        // 1. Calcular s = c1^x mod p
        BigInteger s = c1.modPow(x, p);

        // 2. Calcular m = (c2 * s^-1) mod p, onde s^-1 é o inverso multiplicativo de s
        BigInteger s_inverse = s.modInverse(p);
        BigInteger m = c2.multiply(s_inverse).mod(p);

        return m;
    }

    // Classe para representar a mensagem cifrada (o par c1, c2)
    public static class CifraElGamal {
        private final BigInteger c1;
        private final BigInteger c2;

        public CifraElGamal(BigInteger c1, BigInteger c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        public BigInteger getC1() { return c1; }
        public BigInteger getC2() { return c2; }
    }
}