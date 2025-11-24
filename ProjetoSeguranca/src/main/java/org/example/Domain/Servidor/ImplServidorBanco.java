package org.example.Domain.Servidor;

import org.example.Domain.Model.Entity.Drone;
import org.example.Domain.Service.ServidorService;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImplServidorBanco extends UnicastRemoteObject implements BancoRemoto {

    // O ServidorService armazena a lógica de negócios e os dados (em memória neste projeto)
    private final ServidorService servidorService;
    // Opcional: Manter um mapa para gerenciamento de IDs se necessário, embora ServidorService já agregue a lista.
    private final ConcurrentHashMap<Integer, Drone> drones;

    public ImplServidorBanco() throws RemoteException {
        super();
        this.servidorService = new ServidorService();
        this.drones = new ConcurrentHashMap<>();
        System.out.println("ImplServidorBanco pronto para receber conexões RMI.");
    }

    // --- MÉTODOS DE MANIPULAÇÃO DE DADOS (Persistência) ---

    @Override
    public void adicionarDrone(Drone drone) throws RemoteException {
        System.out.println("RMI: Recebido novo drone para armazenamento. ID: " + drone.getId() + ", Posição: " + drone.getPosicao());
        servidorService.addDrone(drone);
        drones.put(drone.getId(), drone); // Adiciona ao mapa local
    }

    @Override
    public List<Drone> listarTodosDrones() throws RemoteException {
        System.out.println("RMI: Solicitada listagem completa de drones.");
        return servidorService.getAllDrones();
    }

    // --- MÉTODOS DE BUSCA BÁSICA ---

    @Override
    public List<Drone> getDronePosicao(String posicao) throws RemoteException {
        System.out.println("RMI: Solicitada busca por Posição: " + posicao);
        return servidorService.getDronePosicao(posicao);
    }

    @Override
    public List<Drone> getDroneData(String data) throws RemoteException {
        System.out.println("RMI: Solicitada busca por Data: " + data);
        return servidorService.getDroneData(data);
    }

    @Override
    public List<Drone> getDronePosicaoData(String posicao, String data) throws RemoteException {
        System.out.println("RMI: Solicitada busca por Posição e Data: " + posicao + " em " + data);
        return servidorService.getDronePosicaoData(posicao, data);
    }

    // --- IMPLEMENTAÇÃO DOS NOVOS RELATÓRIOS (Análise Nuvem/IA) ---

    /**
     * Relatório 1: Média de Poluentes Críticos por Posição.
     */
    @Override
    public Map<String, String> getMediaPoluentesPorPosicao() throws RemoteException {
        System.out.println("Relatório RMI: Solicitada Média de Poluentes por Posição.");
        return servidorService.getMediaPoluentesPorPosicao();
    }

    /**
     * Relatório 2: Alerta de Tendência de Aumento de Temperatura.
     */
    @Override
    public String getAlertaTendenciaTemperatura() throws RemoteException {
        System.out.println("Relatório RMI: Solicitado Alerta de Tendência de Temperatura.");
        return servidorService.getAlertaTendenciaTemperatura();
    }

    /**
     * Relatório 3: Lista pontos críticos de Ruído.
     */
    @Override
    public List<String> getPontosCriticosRuido(double limiteRuido) throws RemoteException {
        System.out.println("Relatório RMI: Solicitada lista de Pontos Críticos de Ruído (Limite: " + limiteRuido + " dB).");
        return servidorService.getPontosCriticosRuido(limiteRuido);
    }

    /**
     * Relatório 4: Previsão de Qualidade do Ar.
     */
    @Override
    public String getPrevisaoQualidadeAr() throws RemoteException {
        System.out.println("Relatório RMI: Solicitada Previsão de Qualidade do Ar.");
        return servidorService.getPrevisaoQualidadeAr();
    }

    /**
     * Relatório 5: Contagem de Entradas por Posição.
     */
    @Override
    public Map<String, Long> getContagemPorPosicao() throws RemoteException {
        System.out.println("Relatório RMI: Solicitada Contagem de Entradas por Posição.");
        return servidorService.getContagemPorPosicao();
    }
}