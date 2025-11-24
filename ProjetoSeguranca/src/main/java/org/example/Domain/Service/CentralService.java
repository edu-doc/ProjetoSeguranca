package org.example.Domain.Service;

import org.example.API.DTO.DroneDTO;
import org.example.Domain.Model.Entity.Drone;
import org.example.Domain.Servidor.BancoRemoto;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class CentralService {

    private BancoRemoto bancoRemoto;

    public CentralService() {
        conectarBanco();
    }

    private void conectarBanco() {
        try {
            // Conexão RMI direta com o Servidor Banco (porta 1099)
            Registry registro = LocateRegistry.getRegistry("localhost", 1099);
            bancoRemoto = (BancoRemoto) registro.lookup("Banco");
            System.out.println("CentralService: Conectado ao Servidor Banco RMI.");
        } catch (Exception e) {
            System.err.println("Erro na CentralService ao conectar ao banco RMI: " + e.getMessage());
        }
    }

    public void createDrone(DroneDTO droneDTO) {
        Drone drone = new Drone(droneDTO.co2(), droneDTO.co(), droneDTO.no2(), droneDTO.so2(), droneDTO.pm2_5(), droneDTO.pm10(), droneDTO.temperatura(),
                droneDTO.umidade(), droneDTO.ruido(), droneDTO.radiacao(), droneDTO.latitude(), droneDTO.longitude(), droneDTO.posicao());

        try {
            if (bancoRemoto != null) {
                bancoRemoto.adicionarDrone(drone);
                System.out.println("CentralService: Drone " + drone.getId() + " enviado via RMI para o banco.");
            } else {
                System.err.println("Erro: Conexão RMI não estabelecida. Drone " + drone.getId() + " não foi salvo.");
            }
        } catch (RemoteException e) {
            System.err.println("Erro RMI ao adicionar drone: " + e.getMessage());
        }
    }
}