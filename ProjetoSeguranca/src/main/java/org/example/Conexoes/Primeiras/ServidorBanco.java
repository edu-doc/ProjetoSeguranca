package org.example.Conexoes.Primeiras;

import org.example.Domain.Servidor.ImplServidorBanco;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ServidorBanco {
    private static final int PORTA_RMI = 1099;
    private static final String NOME_SERVICO = "Banco";

    public static void main(String[] args) {
        try {
            System.out.println("=== Iniciando Servidor do Banco de Drones ===");
            
            ImplServidorBanco bancoDrones = new ImplServidorBanco();

            Registry registro = LocateRegistry.createRegistry(PORTA_RMI);

            registro.rebind(NOME_SERVICO, bancoDrones);

            System.out.println("Servidor do Banco de Drones iniciado na porta " + PORTA_RMI);
            System.out.println("Nome do serviço: " + NOME_SERVICO);
            System.out.println("Aguardando conexões...");

        } catch (Exception e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
