package org.example.Domain.Servidor;

import org.example.Domain.Model.Entity.Drone;
import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ImplServidorCliente implements Runnable {
    private final Socket socketCliente;
    private final String serverId;
    private BancoRemoto bancoRemoto;
    private static final String HOST = "localhost";
    private static final int PORTA_RMI = 1099;
    private static final String NOME_SERVICO = "Banco";

    public static AtomicInteger conexoesAtivas = null;

    public ImplServidorCliente(String serverId, AtomicInteger conexoesAtivas, Socket socket) {
        this.socketCliente = socket;
        this.serverId = serverId;
        this.conexoesAtivas = conexoesAtivas;
        conectarBanco();
    }

    private void conectarBanco() {
        try {
            Registry registro = LocateRegistry.getRegistry(HOST, PORTA_RMI);
            bancoRemoto = (BancoRemoto) registro.lookup(NOME_SERVICO);
            System.out.println("Conectado ao banco de dados RMI com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro ao conectar ao banco: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enviarAtualizacao() {
        String mensagem = serverId + ":" + conexoesAtivas.decrementAndGet();
        try (MulticastSocket emisorSocket = new MulticastSocket()) {
            byte[] bufferEnvio = mensagem.getBytes();
            DatagramPacket pacoteEnvio = new DatagramPacket(bufferEnvio, bufferEnvio.length, InetAddress.getByName("224.0.0.10"), 55560);
            emisorSocket.send(pacoteEnvio);
            System.out.println("SIMULADOR [" + serverId + "]: Enviou atualização -> " + mensagem);
        } catch (IOException e) {
            System.err.println("SIMULADOR [" + serverId + "]: Erro ao enviar atualização multicast: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            
            // Cria o output primeiro
            ObjectOutputStream objOut = new ObjectOutputStream(socketCliente.getOutputStream());
            objOut.flush();  // força a escrita do cabeçalho

            // Depois cria o input
            ObjectInputStream objIn = new ObjectInputStream(socketCliente.getInputStream());

            // Lê a identificação do cliente
            String clienteId = objIn.readUTF();
            System.out.println("Cliente identificado: " + clienteId);

            if (clienteId.equals("Central")) {
                // Se for a Central, recebe e adiciona o drone diretamente
                Drone droneRecebido = (Drone) objIn.readObject();
                bancoRemoto.adicionarDrone(droneRecebido);
                System.out.println("Drone recebido da Central e adicionado ao banco: " + droneRecebido);
                objOut.writeUTF("Drone adicionado com sucesso!");
                objOut.flush();
            } else if (clienteId.equals("Cliente")) {
                // Se for um cliente normal, processa as opções do menu
                int opcao = -1;  // Inicializa com um valor inválido

                do {
                    try {
                        opcao = objIn.readInt();
                        System.out.println("Opção recebida do cliente: " + opcao);
                        
                        switch (opcao) {
                            case 1:
                                System.out.println("Listando todos os drones...");
                                List<Drone> drones = bancoRemoto.listarTodosDrones();
                                objOut.writeObject(drones);
                                objOut.flush();
                                System.out.println("Drones enviados: " + drones.size());
                                break;

                            case 2:
                                String posicao = objIn.readUTF();
                                System.out.println("Buscando drones na posição: " + posicao);
                                List<Drone> dronesPosicao = bancoRemoto.getDronePosicao(posicao);
                                objOut.writeObject(dronesPosicao);
                                objOut.flush();
                                System.out.println("Drones encontrados: " + dronesPosicao.size());
                                break;

                            case 3:
                                String data = objIn.readUTF();
                                System.out.println("Buscando drones na data: " + data);
                                List<Drone> dronesData = bancoRemoto.getDroneData(data);
                                objOut.writeObject(dronesData);
                                objOut.flush();
                                System.out.println("Drones encontrados: " + dronesData.size());
                                break;

                            case 4:
                                String posicaoCombinada = objIn.readUTF();
                                String dataCombinada = objIn.readUTF();
                                System.out.println("Buscando drones na posição " + posicaoCombinada + " e data " + dataCombinada);
                                List<Drone> dronesCombinados = bancoRemoto.getDronePosicaoData(posicaoCombinada, dataCombinada);
                                objOut.writeObject(dronesCombinados);
                                objOut.flush();
                                System.out.println("Drones encontrados: " + dronesCombinados.size());
                                break;

                            case 0:
                                objOut.writeUTF("Encerrando conexão...");
                                objOut.flush();
                                break;

                            default:
                                objOut.writeUTF("Opção inválida!");
                                objOut.flush();
                        }
                    } catch (Exception e) {
                        System.err.println("Erro ao processar opção: " + e.getMessage());
                        e.printStackTrace();
                        objOut.writeUTF("Erro ao processar operação: " + e.getMessage());
                        objOut.flush();
                    }
                } while (opcao != 0);
            }

            objIn.close();
            objOut.close();
            socketCliente.close();

        } catch (SocketTimeoutException e) {
            System.err.println("Timeout com cliente: " + socketCliente.getInetAddress());
            e.printStackTrace();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro com cliente: " + e.getMessage());
            e.printStackTrace();
        } finally {
            enviarAtualizacao();
        }
    }
}