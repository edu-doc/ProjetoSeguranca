package org.example.Domain.Model.Entity;

import org.example.Auxiliar.Cripto.ImplArgon2;
import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    // Mapa: <username, Argon2 Hash>
    private static final Map<String, String> USERS = new HashMap<>();

    // --- Credenciais Válidas (Login de Sucesso) ---
    private static final String USER_VALIDO = "cliente_principal";
    private static final String SENHA_VALIDA = "senha_segura_123";

    // --- Credenciais Inválidas (Teste de Falha - Usadas no Cliente) ---
    private static final String USER_FALHA = "dispositivo_invalido";
    private static final String SENHA_FALHA = "chave_errada_456"; // Será usada como a senha inválida de teste

    static {
        // 1. Gera e armazena o hash seguro (Argon2) APENAS para o usuário válido.
        USERS.put(USER_VALIDO, ImplArgon2.gerarHash(SENHA_VALIDA.toCharArray()));

        // **IMPORTANTE:** Não adicionamos o USER_FALHA ao mapa.
        // O método getHashByUsername(USER_FALHA) retornará null, forçando a falha de login.

        System.out.println("UserRepository: Repositório de usuários simulado inicializado.");
        System.out.println("Hash do usuário " + USER_VALIDO + " gerado com Argon2.");
    }

    // --- Métodos de Acesso ---

    public static String getHashByUsername(String username) {
        // Retorna o hash se o usuário for válido, ou null se for o dispositivo_invalido
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