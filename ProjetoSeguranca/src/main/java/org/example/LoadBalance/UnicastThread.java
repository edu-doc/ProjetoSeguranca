package org.example.LoadBalance;

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
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            if (servidores.isEmpty()) {
                out.println("Nenhum servidor disponível no momento.");
                return;
            }

            ServerInfo servidorEscolhido = null;
            int minConexoes = Integer.MAX_VALUE; // Maior valor possivel dos inteiros, usado em comparação

            for (ServerInfo servidorCandidato : servidores) {
                if (servidorCandidato.getConexoesAtivas() * servidorCandidato.getPeso() < minConexoes) {
                    minConexoes = servidorCandidato.getConexoesAtivas() * servidorCandidato.getPeso();
                    servidorEscolhido = servidorCandidato;
                }
            }

            if (servidorEscolhido == null && !servidores.isEmpty()) {
                servidorEscolhido = servidores.get(0);
            }


            if (servidorEscolhido != null) {
                out.println(servidorEscolhido.getPorta());
                System.out.println("[Unicast] Cliente redirecionado para porta: " + servidorEscolhido.getPorta());
            } else {
                out.println("Erro ao encontrar servidor.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
