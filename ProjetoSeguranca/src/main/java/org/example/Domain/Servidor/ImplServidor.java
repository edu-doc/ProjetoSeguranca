package org.example.Domain.Servidor;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class ImplServidor implements Runnable {
    private final String serverId;

    private String grupo = "224.0.0.10";
    private final int porta = 55560;
    private DatagramSocket emisorSocket; // Socket para enviar

    public static AtomicInteger conexoesAtivas = null;
    private boolean operacaoEnvio;

    public ImplServidor(String serverId, boolean operacaoEnvio , AtomicInteger conexoesAtivas) throws IOException {
        this.serverId = serverId;
        this.emisorSocket = new DatagramSocket();
        this.operacaoEnvio = operacaoEnvio;
        this.conexoesAtivas = conexoesAtivas;
    }

    public void NovaConexao() {
        conexoesAtivas.incrementAndGet();
        enviarAtualizacao();
    }

    public void ConexaoFechada() {
        if (conexoesAtivas.get() > 0) {
            conexoesAtivas.decrementAndGet();;
        }
        enviarAtualizacao();
    }

    private void enviarAtualizacao() {
        String mensagem = serverId + ":" + conexoesAtivas;
        try {
            byte[] bufferEnvio = mensagem.getBytes();
            DatagramPacket pacoteEnvio = new DatagramPacket(bufferEnvio, bufferEnvio.length, InetAddress.getByName(grupo), porta);
            emisorSocket.send(pacoteEnvio);
            System.out.println("SIMULADOR [" + serverId + "]: Enviou atualização -> " + mensagem);
        } catch (IOException e) {
            System.err.println("SIMULADOR [" + serverId + "]: Erro ao enviar atualização multicast: " + e.getMessage());
        }
    }

    public void close() {
        if (emisorSocket != null) {
            emisorSocket.close();
        }
    }

    @Override
    public void run() {
        if (operacaoEnvio) {
            NovaConexao();
        } else {
            ConexaoFechada();
        }
        
        close();
    }
}