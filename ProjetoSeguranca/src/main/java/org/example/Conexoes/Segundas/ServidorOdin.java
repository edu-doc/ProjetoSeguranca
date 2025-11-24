package org.example.Conexoes.Segundas;

import org.example.Domain.Servidor.ImplServidor;
import org.example.Domain.Servidor.ImplServidorCliente;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class ServidorOdin {

    private static final int PORT = 54321;
    private static final int MAX_THREADS = 100;
    public static final AtomicInteger conexoesAtivas = new AtomicInteger(0);


    public static void main(String[] args) {

        // Inicia o serviço de multicast
        //try {
        //    ImplServidor implServidor = new ImplServidor("S2", "224.0.0.10", 55560);
        //    Thread multicastThread = new Thread(implServidor);
        //    multicastThread.start();
        //    System.out.println("Thread multicast iniciada.");
        //} catch (Exception e) {
        //    System.err.println("Erro ao iniciar multicast: " + e.getMessage());
        //}

        // Inicia o servidor TCP com pool de threads
        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor TCP iniciado na porta " + PORT);

            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Cliente conectado: " + socketCliente.getInetAddress());
                pool.execute(new ImplServidorCliente("S2", conexoesAtivas ,socketCliente));
                pool.execute(new ImplServidor("S2", true, conexoesAtivas));
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }
}
