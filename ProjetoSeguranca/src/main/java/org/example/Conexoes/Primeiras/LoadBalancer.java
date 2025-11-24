package org.example.Conexoes.Primeiras;

import org.example.LoadBalance.MulticastThread;
import org.example.LoadBalance.ServerInfo;
import org.example.LoadBalance.UnicastThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class LoadBalancer {

    static ServerSocket socketServidor;
    static int PORTA = 22234;

    public static void main(String[] args) throws IOException {
        CopyOnWriteArrayList<ServerInfo> servidores = new CopyOnWriteArrayList<>();

        ServerInfo srvInfo1 = new ServerInfo("S1", "10.0.0.1", 12345, 3);
        ServerInfo srvInfo2 = new ServerInfo("S2", "10.0.0.2", 54321, 1);

        // ServerInfo srvInfo1 = new ServerInfo("S1", "10.0.0.1", 12345, 1);
        // ServerInfo srvInfo2 = new ServerInfo("S2", "10.0.0.2", 54321, 1);

        servidores.add(srvInfo1);
        servidores.add(srvInfo2);

        Thread multicastThread = new Thread(new MulticastThread(servidores));

        multicastThread.start();

        socketServidor = new ServerSocket(PORTA);

        System.out.println("Servidor rodando na porta " +
                socketServidor.getLocalPort());
        System.out.println("HostAddress = " +
                InetAddress.getLocalHost().getHostAddress());
        System.out.println("HostName = " +
                InetAddress.getLocalHost().getHostName());

        while (true) {
            Socket clientSocket = socketServidor.accept();
            System.out.println("[Unicast] Novo cliente conectado: " + clientSocket.getInetAddress());

            Thread unicastThread = new Thread(new UnicastThread(clientSocket, servidores));
            unicastThread.start();
        }
    }
}
