package org.example.LoadBalance;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MulticastThread implements Runnable {

    private final CopyOnWriteArrayList<ServerInfo> servidores;

    private final MulticastSocket multiSocket;
    private final InetAddress multicastIP = InetAddress.getByName("224.0.0.17");
    private final InetSocketAddress grupo;
    private final NetworkInterface interfaceRede = NetworkInterface.getByName("ethernet_32768");
    private final int porta = 55560;

    private volatile boolean rodando = true;

    public MulticastThread(CopyOnWriteArrayList<ServerInfo> servidores) throws IOException {
        this.servidores = servidores;

        multiSocket = new MulticastSocket(this.porta);

        System.out.println("Receptor " +
                InetAddress.getLocalHost() +
                " executando na porta " +
                multiSocket.getLocalPort());

        grupo = new InetSocketAddress(this.multicastIP, this.porta);

        if (interfaceRede == null) {
            throw new IOException("A interface de rede não foi encontrada.");
        }

        System.out.println("Balanceador de Carga: Juntando-se ao grupo " + this.multicastIP.getHostAddress() +
                " na porta " + this.porta + " interface: " + interfaceRede.getDisplayName() + ".");

        multiSocket.joinGroup(grupo, interfaceRede);
    }

    @Override
    public void run() {
        System.out.println("Balanceador de Carga: Listener multicast iniciado em " +
                this.multicastIP.getHostAddress() + ":" + this.multiSocket.getLocalPort());

        byte[] buffer = new byte[1024];

        while (rodando) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            try {
                multiSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength()).trim();

                String[] parts = message.split(":", 2);
                if (parts.length == 2) {
                    String serverId = parts[0];
                    try {
                        int quantidadeConexoes = Integer.parseInt(parts[1]);
                        System.out.println("[Multicast] Recebido: " + message);
                        atualizarContagemServidor(serverId, quantidadeConexoes);
                    } catch (NumberFormatException e) {
                        System.err.println("Thread de Balanceamento: Número inválido de conexões: '" + parts[1] + "'");
                    }
                } else {
                    System.err.println("Thread de Balanceamento: Formato inválido: '" + message + "'. Esperado 'ID:Contagem'.");
                }

            } catch (IOException e) {
                if (rodando) {
                    System.err.println("Erro ao receber pacote multicast: " + e.getMessage());
                }
            }
        }

        try {
            multiSocket.leaveGroup(grupo, interfaceRede);
        } catch (IOException e) {
            System.err.println("Erro ao sair do grupo multicast: " + e.getMessage());
        }

        multiSocket.close();
        System.out.println("Thread de Balanceamento: Encerrada.");
    }

    protected void atualizarContagemServidor(String serverId, int novaContagem) {
        boolean encontrado = false;
        for (ServerInfo server : servidores) {
            if (server.getId().equals(serverId)) {
                server.setConexoesAtivas(novaContagem);
                System.out.println("[Multicast] Atualizado " + serverId + " -> " + novaContagem + " conexões.");
                encontrado = true;
                break;
            }
        }
        if (!encontrado) {
            System.err.println("[Multicast] Servidor com ID '" + serverId + "' não encontrado na lista.");
        }
    }
}
