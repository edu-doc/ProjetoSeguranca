package org.example.LoadBalance;

import org.example.Auxiliar.Cripto.ImplArgon2;
import org.example.Auxiliar.Cripto.ImplJwt;
import org.example.Domain.Model.Entity.UserRepository;
import io.jsonwebtoken.JwtException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class UnicastThread implements Runnable {
    private final Socket socket;
    private final CopyOnWriteArrayList<ServerInfo> servidores;

    public UnicastThread(Socket socket, CopyOnWriteArrayList<ServerInfo> servidores) {
        this.socket = socket;
        this.servidores = servidores;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            // Handshake inicial: Cliente envia o tipo de requisição (LOGIN: ou SERVICE:)
            String tipoRequisicao = in.readLine();

            if (tipoRequisicao == null) return;

            System.out.println("[Unicast] Requisição recebida: " + tipoRequisicao.split(":")[0]);

            if (tipoRequisicao.startsWith("LOGIN:")) {

                // --- 1. PROCESSO DE AUTENTICAÇÃO (Argon2) ---
                String credenciais = tipoRequisicao.substring(6);
                String[] parts = credenciais.split(",");

                if (parts.length != 2) {
                    out.println("ERRO_AUTH: Formato de credenciais inválido.");
                    return;
                }

                String username = parts[0];
                char[] password = parts[1].toCharArray();

                String userHash = UserRepository.getHashByUsername(username);

                if (userHash != null && ImplArgon2.verificar(userHash, password)) {
                    // Autenticação bem-sucedida (Argon2 + Salt)
                    String jwt = ImplJwt.gerarToken(username, "cliente");
                    out.println("TOKEN_OK:" + jwt);
                    System.out.println("✅ [Unicast] Login bem-sucedido para " + username + ". JWT emitido.");
                } else {
                    // Simulação de credenciais inválidas (REQUISITO DO TRABALHO)
                    ImplArgon2.limparArray(password);
                    out.println("ERRO_AUTH: Credenciais inválidas.");
                    System.err.println("❌ [Unicast] Falha de login para " + username + ". Conexão rejeitada.");
                }

            } else if (tipoRequisicao.startsWith("SERVICE:")) {

                // --- 2. PROCESSO DE REDIRECIONAMENTO COM VALIDAÇÃO JWT ---
                String jwtToken = tipoRequisicao.substring(8);

                try {
                    // Valida o JWT antes de redirecionar para o balanceamento de carga
                    ImplJwt.validarToken(jwtToken);

                    if (servidores.isEmpty()) {
                        out.println("ERRO_SERVER: Nenhum servidor disponível no momento.");
                        return;
                    }

                    // Lógica de Load Balance (Least Connections * Weight)
                    ServerInfo servidorEscolhido = null;
                    long minCusto = Long.MAX_VALUE;

                    for (ServerInfo servidorCandidato : servidores) {
                        long custo = (long)servidorCandidato.getConexoesAtivas() * servidorCandidato.getPeso();
                        if (custo < minCusto) {
                            minCusto = custo;
                            servidorEscolhido = servidorCandidato;
                        }
                    }

                    if (servidorEscolhido != null) {
                        out.println("PORT_OK:" + servidorEscolhido.getPorta());
                        System.out.println("➡️ [Unicast] Cliente redirecionado para porta: " + servidorEscolhido.getPorta());
                    } else {
                        out.println("ERRO_SERVER: Erro ao encontrar servidor.");
                    }

                } catch (JwtException e) {
                    out.println("ERRO_AUTH: Token JWT inválido ou expirado. " + e.getMessage());
                    System.err.println("❌ [Unicast] Requisição de serviço rejeitada devido a JWT inválido: " + e.getMessage());
                }

            } else {
                // Protocolo da Central original (agora obsoleto) ou cliente que enviou lixo
                out.println("ERRO_PROTO: Protocolo de requisição desconhecido.");
            }

        } catch (Exception e) {
            System.err.println("Erro na Thread Unicast: " + e.getMessage());
        }
    }
}