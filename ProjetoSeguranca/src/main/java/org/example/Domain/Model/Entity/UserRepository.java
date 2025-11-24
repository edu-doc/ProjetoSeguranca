package org.example.Domain.Model.Entity;

import org.example.Auxiliar.Cripto.ImplArgon2;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    // Mapa: <username, Argon2 Hash>
    // Este mapa simula o armazenamento seguro de senhas no Datacenter/Banco.
    private static final Map<String, String> USERS = new HashMap<>();

    // --- Credenciais Válidas (Login de Sucesso) ---
    private static final String USER_VALIDO = "cliente_principal";
    private static final String SENHA_VALIDA = "senha_segura_123";

    // --- Credenciais Inválidas (Teste de Falha - Requisito do Projeto) ---
    private static final String USER_FALHA = "dispositivo_invalido";
    private static final String SENHA_FALHA = "chave_errada_456";

    static {
        // 1. Gera o hash seguro (Argon2) para o usuário principal no momento da inicialização.
        USERS.put(USER_VALIDO, ImplArgon2.gerarHash(SENHA_VALIDA.toCharArray()));

        // 2. Adiciona o usuário inválido. A senha armazenada não importa,
        // pois o teste de falha ocorre ao tentar usar a SENHA_FALHA, que não
        // corresponderá ao hash.
        USERS.put(USER_FALHA, ImplArgon2.gerarHash(SENHA_FALHA.toCharArray()));

        System.out.println("UserRepository: Repositório de usuários simulado inicializado.");
        System.out.println("Hash do usuário " + USER_VALIDO + " gerado com Argon2.");
    }

    // --- Métodos de Acesso ---

    public static String getHashByUsername(String username) {
        return USERS.get(username);
    }

    public static String getValidUsername() {
        return USER_VALIDO;
    }

    public static String getValidPassword() {
        return SENHA_VALIDA;
    }

    public static String getInvalidUsername() {
        return USER_FALHA;
    }

    public static String getInvalidPassword() {
        return SENHA_FALHA;
    }
}