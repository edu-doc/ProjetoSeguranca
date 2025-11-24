package org.example.Domain.Servidor;

import org.example.Domain.Model.Entity.Drone;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map; // Importado para Map

public interface BancoRemoto extends Remote {
    void adicionarDrone(Drone drone) throws RemoteException;
    List<Drone> listarTodosDrones() throws RemoteException;
    List<Drone> getDronePosicao(String posicao) throws RemoteException;
    List<Drone> getDroneData(String Data) throws RemoteException;
    List<Drone> getDronePosicaoData(String posicao, String Data) throws RemoteException;

    Map<String, String> getMediaPoluentesPorPosicao() throws RemoteException; // Relatório 1
    String getAlertaTendenciaTemperatura() throws RemoteException; // Relatório 2
    List<String> getPontosCriticosRuido(double limiteRuido) throws RemoteException; // Relatório 3
    String getPrevisaoQualidadeAr() throws RemoteException; // Relatório 4
    Map<String, Long> getContagemPorPosicao() throws RemoteException; // Relatório 5
}