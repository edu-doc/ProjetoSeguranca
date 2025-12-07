package org.example.Seguranca;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class IDS {
    private static final int PORTA_IDS = 6000;

    public static void main(String[] args) {
        System.out.println("=== [IDS] Sistema de Detecção de Intrusão Iniciado ===");
        System.out.println("Monitorando alertas na porta " + PORTA_IDS + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORTA_IDS)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ManipuladorAlerta(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ManipuladorAlerta implements Runnable {
        private Socket socket;

        public ManipuladorAlerta(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String alerta = in.readLine();
                if (alerta != null) {
                    System.out.println("🚨 [IDS] ALERTA RECEBIDO: " + alerta);
                    
                    // Lógica de Resposta a Incidentes
                    if (alerta.contains("VALOR_ANOMALO")) {
                        // Exemplo de alerta: "VALOR_ANOMALO:Origem:Norte"
                        String[] parts = alerta.split(":");
                        String origem = parts[2]; // Pega a posição/ID
                        
                        System.out.println("⚠️ [IDS] Detectada anomalia crítica de " + origem + ".");
                        System.out.println("🛡️ [IDS] Enviando comando de BLOQUEIO para o Firewall de Borda...");
                        
                        // Responde ao solicitante (Borda) para bloquear
                        out.println("BLOCK:" + origem);
                    } else {
                        System.out.println("ℹ️ [IDS] Log registrado. Nenhuma ação de bloqueio imediata.");
                        out.println("ACK");
                    }
                }
            } catch (IOException e) {
                System.err.println("[IDS] Erro ao processar alerta: " + e.getMessage());
            }
        }
    }
}