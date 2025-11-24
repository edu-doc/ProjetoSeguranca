package org.example.Domain.Servidor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.example.Auxiliar.Cripto.ImplJwt;
import org.example.Domain.Model.Entity.Drone;
import java.io.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
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

            ObjectOutputStream objOut = new ObjectOutputStream(socketCliente.getOutputStream());
            objOut.flush();

            ObjectInputStream objIn = new ObjectInputStream(socketCliente.getInputStream());

            String clienteId = objIn.readUTF();
            System.out.println("Cliente identificado: " + clienteId);

            if (clienteId.equals("Central")) {
                // ... (Lógica da Central, removida por ser obsoleta, mas mantida a compatibilidade)
                Drone droneRecebido = (Drone) objIn.readObject();
                bancoRemoto.adicionarDrone(droneRecebido);
                System.out.println("Drone recebido da Central e adicionado ao banco: " + droneRecebido);
                objOut.writeUTF("Drone adicionado com sucesso!");
                objOut.flush();
            } else if (clienteId.equals("Cliente")) {

                // --- VALIDAÇÃO JWT (CAMADA DE AUTORIZAÇÃO) ---
                String jwtToken = objIn.readUTF(); // LER O TOKEN ENVIADO PELO CLIENTE
                Claims claims = ImplJwt.validarToken(jwtToken);
                String username = claims.getSubject();
                System.out.println("✅ Autorização JWT validada para usuário: " + username);
                // ---------------------------------------------

                int opcao = -1;

                do {
                    // ... (resto da lógica de leitura de opção e processamento do menu)

                    // Código de processamento do menu (opções 1-9)
                    try {
                        opcao = objIn.readInt();
                        System.out.println("Opção recebida do cliente (" + username + "): " + opcao);

                        switch (opcao) {
                            case 1: // Listar Todos
                                List<Drone> drones = bancoRemoto.listarTodosDrones();
                                objOut.writeObject(drones);
                                break;
                            case 2: // Buscar Posição
                                String posicao = objIn.readUTF();
                                List<Drone> dronesPosicao = bancoRemoto.getDronePosicao(posicao);
                                objOut.writeObject(dronesPosicao);
                                break;
                            case 3: // Buscar Data
                                String data = objIn.readUTF();
                                List<Drone> dronesData = bancoRemoto.getDroneData(data);
                                objOut.writeObject(dronesData);
                                break;
                            case 4: // Buscar Posição e Data
                                String posicaoCombinada = objIn.readUTF();
                                String dataCombinada = objIn.readUTF();
                                List<Drone> dronesCombinados = bancoRemoto.getDronePosicaoData(posicaoCombinada, dataCombinada);
                                objOut.writeObject(dronesCombinados);
                                break;
                            case 5: // Média Poluentes
                                Map<String, String> medias = bancoRemoto.getMediaPoluentesPorPosicao();
                                objOut.writeObject(medias);
                                break;
                            case 6: // Tendência Temp
                                String alerta = bancoRemoto.getAlertaTendenciaTemperatura();
                                objOut.writeObject(alerta);
                                break;
                            case 7: // Contagem Posição
                                Map<String, Long> contagem = bancoRemoto.getContagemPorPosicao();
                                objOut.writeObject(contagem);
                                break;
                            case 8: // Previsão Qualidade Ar
                                String previsao = bancoRemoto.getPrevisaoQualidadeAr();
                                objOut.writeObject(previsao);
                                break;
                            case 9: // Pontos Críticos Ruído
                                double limite = objIn.readDouble();
                                List<String> pontos = bancoRemoto.getPontosCriticosRuido(limite);
                                objOut.writeObject(pontos);
                                break;
                            case 0:
                                objOut.writeUTF("Encerrando conexão...");
                                break;
                            default:
                                objOut.writeUTF("Opção inválida!");
                        }
                        objOut.flush();
                    } catch (Exception e) {
                        System.err.println("Erro ao processar opção: " + e.getMessage());
                        objOut.writeUTF("ERRO_OP: Erro ao processar operação: " + e.getMessage());
                        objOut.flush();
                    }
                } while (opcao != 0);
            }

            objIn.close();
            objOut.close();
            socketCliente.close();

        } catch (JwtException e) {
            System.err.println("❌ ERRO FATAL DE AUTORIZAÇÃO: JWT inválido ou expirado. Encerrando conexão. Detalhe: " + e.getMessage());
            try {
                // Responde o erro antes de fechar a conexão, para o cliente capturar a falha de segurança.
                new ObjectOutputStream(socketCliente.getOutputStream()).writeUTF("ERRO_AUTH: Token JWT inválido ou expirado. Requisite novo login.");
            } catch (IOException ex) {}
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro com cliente: " + e.getMessage());
        } finally {
            enviarAtualizacao();
        }
    }
}