package org.example.Conexoes.Segundas;

import org.example.Domain.Servidor.ImplServidor;
import org.example.Domain.Servidor.ImplServidorCliente;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ServidorZeus {
    private static final int PORT = 12345;
    private static final int MAX_THREADS = 100;
    private static final int TIMEOUT = 30000; // 30 segundos
    public static final AtomicInteger conexoesAtivas = new AtomicInteger(0);


    public static void main(String[] args) {
        // Inicia multicast
        // try {
        //    ImplServidor implServidor = new ImplServidor("S1", "224.0.0.10", 55560);
        //    Thread multicastThread = new Thread(implServidor);
        //    multicastThread.start();
        //    System.out.println("Serviço multicast iniciado.");
        //} catch (Exception e) {
        //    System.err.println("Erro no multicast: " + e.getMessage());
        //}

        // Configura pool de threads
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor TCP iniciado na porta " + PORT);

            while (!Thread.interrupted()) {
                try {
                    Socket socketCliente = serverSocket.accept();
                    System.out.println("Novo cliente: " + socketCliente.getInetAddress());
                    pool.execute(new ImplServidorCliente("S1", conexoesAtivas ,socketCliente));
                    pool.execute(new ImplServidor("S1", true, conexoesAtivas));
                } catch (IOException e) {
                    System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }
}