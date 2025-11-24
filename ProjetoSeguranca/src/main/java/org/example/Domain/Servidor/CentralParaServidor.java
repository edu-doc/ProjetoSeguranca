package org.example.Domain.Servidor;

import org.example.Domain.Model.Entity.Drone;

import java.io.*;
import java.net.Socket;

public class CentralParaServidor {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 22234;

    public void conexaoCentralServidor(Drone drone) {
        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {

            System.out.println("Conectando com Load Balancer na porta " + SERVER_PORT);

            String porta = in.readLine();
            System.out.println("Load Balancer retornou a porta: " + porta);
            int novaPorta = Integer.parseInt(porta);

            try (
                    Socket novoSocket = new Socket(SERVER_ADDRESS, novaPorta);
                    ObjectOutputStream objOut = new ObjectOutputStream(novoSocket.getOutputStream());
                    ObjectInputStream objIn = new ObjectInputStream(novoSocket.getInputStream())
            ) {
                System.out.println("Redirecionado para o servidor na porta " + novaPorta);

                // Identifica como Central e envia o drone
                objOut.writeUTF("Central");
                objOut.flush();
                objOut.writeObject(drone);
                objOut.flush();

                // Aguarda confirmação
                String confirmacao = objIn.readUTF();
                System.out.println("Resposta do servidor: " + confirmacao);

            } catch (IOException e) {
                System.out.println("Erro ao conectar na nova porta: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (IOException e) {
            System.out.println("Erro na conexão inicial: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
