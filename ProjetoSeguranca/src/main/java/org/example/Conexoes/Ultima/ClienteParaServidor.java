package org.example.Conexoes.Ultima;

import org.example.Domain.Model.Entity.Drone;
import org.example.Domain.Model.Entity.UserRepository;
import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ClienteParaServidor {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int LB_PORT = 22234; // Load Balancer Port
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String jwtToken = null; // Token de sessão

    public void conexaoCentralServidor() {

        // --- ETAPA 1: AUTENTICAÇÃO (LOGIN) ---
        if (!autenticarCliente(UserRepository.getValidUsername(), UserRepository.getValidPassword())) {
            System.out.println("Autenticação inicial falhou. Encerrando programa.");
            return;
        }

        // --- ETAPA 2: REDIRECIONAMENTO ---
        int novaPorta = solicitarRedirecionamento();
        if (novaPorta != -1) {
            // --- ETAPA 3: OPERAÇÕES NO SERVIDOR DE BORDA (DATACENTER) ---
            executarOperacoes(novaPorta);
        } else {
            System.out.println("Falha crítica ao obter porta de redirecionamento. Encerrando.");
        }

        // --- ETAPA 4: SIMULAÇÃO DE FALHA (REQUISITO DO TRABALHO) ---
        simularAcessoInvalido();
    }

    private boolean autenticarCliente(String username, String password) {
        System.out.println("\n=== Tentando LOGIN como Cliente: " + username + " ===");
        try (
                Socket socket = new Socket(SERVER_ADDRESS, LB_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("LOGIN:" + username + "," + password);
            String resposta = in.readLine();

            if (resposta != null && resposta.startsWith("TOKEN_OK:")) {
                jwtToken = resposta.substring(9);
                System.out.println("✅ Login bem-sucedido! Token JWT recebido.");
                return true;
            } else {
                System.err.println("❌ Falha no login: " + (resposta != null ? resposta : "Resposta vazia."));
                return false;
            }

        } catch (IOException e) {
            System.err.println("Erro na conexão inicial (Login): " + e.getMessage());
            return false;
        }
    }

    private int solicitarRedirecionamento() {
        System.out.println("\n=== Solicitando Redirecionamento com JWT ===");
        try (
                Socket socket = new Socket(SERVER_ADDRESS, LB_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            out.println("SERVICE:" + jwtToken);
            String resposta = in.readLine();

            if (resposta != null && resposta.startsWith("PORT_OK:")) {
                String portaStr = resposta.substring(8);
                int novaPorta = Integer.parseInt(portaStr);
                System.out.println("✅ Redirecionamento autorizado. Nova porta: " + novaPorta);
                return novaPorta;
            } else {
                System.err.println("❌ Falha no redirecionamento: " + (resposta != null ? resposta : "Resposta vazia."));
                return -1;
            }

        } catch (IOException | NumberFormatException e) {
            System.err.println("Erro na conexão/parsing (Redirecionamento): " + e.getMessage());
            return -1;
        }
    }

    private void simularAcessoInvalido() {
        System.out.println("\n\n--- REQUISITO: SIMULAÇÃO DE ACESSO INVÁLIDO ---");
        System.out.println("Tentando LOGIN com credenciais inválidas (Dispositivo/Cliente Mal-intencionado)...");
        // O Load Balancer rejeitará esta tentativa (ERRO_AUTH), cumprindo o requisito.
        autenticarCliente(UserRepository.getInvalidUsername(), UserRepository.getInvalidPassword());
    }


    private void executarOperacoes(int novaPorta) {
        System.out.println("\n=== Executando Operações no Servidor de Borda (" + novaPorta + ") ===");
        try (
                Socket novoSocket = new Socket(SERVER_ADDRESS, novaPorta);
                ObjectOutputStream objOut = new ObjectOutputStream(novoSocket.getOutputStream());
                ObjectInputStream objIn = new ObjectInputStream(novoSocket.getInputStream())
        ) {
            // Identifica como Cliente e envia o token para validação no ImplServidorCliente
            objOut.writeUTF("Cliente");
            objOut.flush();
            objOut.writeUTF(jwtToken); // Envia o JWT
            objOut.flush();

            Scanner scanner = new Scanner(System.in);
            int opcao = -1;

            do {
                mostrarMenu();
                try {
                    opcao = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Por favor, digite um número válido.");
                    continue;
                }

                // Envia a opção escolhida
                objOut.writeInt(opcao);
                objOut.flush();

                if (opcao != 0) {
                    try {
                        switch (opcao) {
                            case 1: // Listar Todos
                            case 2: // Buscar Posição
                            case 3: // Buscar Data
                            case 4: // Buscar Posição e Data
                                if (opcao == 2) {
                                    System.out.print("Digite a posição (Norte, Sul, Leste, Oeste): ");
                                    String posicao = scanner.nextLine().trim();
                                    objOut.writeUTF(posicao);
                                    objOut.flush();
                                } else if (opcao == 3) {
                                    System.out.print("Digite a data (dd/MM/yyyy): ");
                                    String data = scanner.nextLine().trim();
                                    try {
                                        LocalDate.parse(data, formatter);
                                        objOut.writeUTF(data);
                                        objOut.flush();
                                    } catch (DateTimeParseException e) {
                                        System.out.println("Formato de data inválido. Use dd/MM/yyyy");
                                        continue;
                                    }
                                } else if (opcao == 4) {
                                    System.out.print("Digite a posição (Norte, Sul, Leste, Oeste): ");
                                    String posicaoCombinada = scanner.nextLine().trim();
                                    System.out.print("Digite a data (dd/MM/yyyy): ");
                                    String dataCombinada = scanner.nextLine().trim();
                                    try {
                                        LocalDate.parse(dataCombinada, formatter);
                                        objOut.writeUTF(posicaoCombinada);
                                        objOut.flush();
                                        objOut.writeUTF(dataCombinada);
                                        objOut.flush();
                                    } catch (DateTimeParseException e) {
                                        System.out.println("Formato de data inválido. Use dd/MM/yyyy");
                                        continue;
                                    }
                                }
                                processarResposta(objIn);
                                break;

                            // --- NOVOS RELATÓRIOS (Nuvem/IA) ---
                            case 5: // Média Poluentes (Não precisa de dados de entrada)
                            case 6: // Tendência Temp (Não precisa de dados de entrada)
                            case 7: // Contagem Posição (Não precisa de dados de entrada)
                            case 8: // Previsão Qualidade Ar (Não precisa de dados de entrada)
                                processarResposta(objIn);
                                break;
                            case 9: // Pontos Críticos Ruído (Precisa de limite)
                                System.out.print("Digite o limite de ruído (ex: 80.0): ");
                                try {
                                    double limite = Double.parseDouble(scanner.nextLine().trim());
                                    objOut.writeDouble(limite);
                                    objOut.flush();
                                    processarResposta(objIn);
                                } catch (NumberFormatException e) {
                                    System.out.println("Entrada inválida. Use um número.");
                                    continue;
                                }
                                break;

                            default:
                                System.out.println("Opção inválida!");
                        }
                    } catch (ClassNotFoundException e) {
                        System.err.println("Erro ao processar resposta: " + e.getMessage());
                    } catch (IOException e) {
                        System.err.println("Erro na comunicação com o servidor: " + e.getMessage());
                        return;
                    }
                }
            } while (opcao != 0);

        } catch (IOException e) {
            System.err.println("Erro ao conectar no servidor de borda na porta " + novaPorta + ": " + e.getMessage());
        }
    }

    private void processarResposta(ObjectInputStream objIn) throws IOException, ClassNotFoundException {
        Object resposta = objIn.readObject();

        System.out.println("\n--- Resposta do Datacenter/Nuvem ---");

        if (resposta instanceof List<?>) {
            List<?> lista = (List<?>) resposta;
            if (lista.isEmpty()) {
                System.out.println("Nenhum dado/relatório encontrado.");
            } else {
                for (Object o : lista) {
                    // Trata lista de Drones ou lista de Strings (Pontos Críticos)
                    if (o instanceof Drone) {
                        System.out.println(o);
                    } else {
                        System.out.println(o.toString()); // Lista de Strings
                    }
                }
            }
        } else if (resposta instanceof Map) {
            System.out.println("Relatório de Agregação:");
            ((Map<?,?>) resposta).forEach((k, v) -> System.out.println("- " + k + ": " + v));
        } else {
            // Trata Strings (Alertas/Previsão)
            System.out.println("Relatório/Alerta: " + resposta.toString());
        }
        System.out.println("------------------------------------");
    }

    private void mostrarMenu() {
        System.out.println("\n=== Menu do Cliente (Acesso ao Datacenter/Nuvem) ===");
        System.out.println("--- Consultas Básicas ---");
        System.out.println("1. Listar Todos os Drones");
        System.out.println("2. Buscar Drones por Posição");
        System.out.println("3. Buscar Drones por Data");
        System.out.println("4. Buscar Drones por Posição e Data");
        System.out.println("--- Relatórios de Análise (Nuvem/IA) ---");
        System.out.println("5. Média de Poluentes Críticos por Posição (Relatório 1)");
        System.out.println("6. Alerta de Tendência de Aumento de Temperatura (Relatório 2)");
        System.out.println("7. Contagem de Entradas por Posição (Relatório 5)");
        System.out.println("8. Previsão Simples de Qualidade do Ar (Relatório 4)");
        System.out.println("9. Listar Pontos Críticos de Ruído (Relatório 3)");
        System.out.println("0. Sair");
        System.out.print("Escolha uma opção: ");
    }

    public static void main(String[] args) {
        new ClienteParaServidor().conexaoCentralServidor();
    }
}