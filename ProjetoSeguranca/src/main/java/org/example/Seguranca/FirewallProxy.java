package org.example.Seguranca;

import org.example.API.DTO.DroneDTO;

public class FirewallProxy {

    // Regras de negócio para detecção de anomalias (Signature/Anomaly Detection)
    private static final double MAX_TEMP = 80.0;
    private static final double MAX_CO2 = 5000.0;
    private static final double MAX_RUIDO = 150.0;

    public static void inspecionarPacote(DroneDTO drone) throws SecurityException {
        // Inspeção Profunda de Pacote (DPI - simulada nos dados decifrados)
        
        StringBuilder violações = new StringBuilder();
        boolean anomaliaDetectada = false;

        if (drone.temperatura() > MAX_TEMP) {
            violações.append(String.format("Temperatura Extrema (%.2f); ", drone.temperatura()));
            anomaliaDetectada = true;
        }
        if (drone.co2() > MAX_CO2) {
            violações.append(String.format("Nível CO2 Crítico (%.2f); ", drone.co2()));
            anomaliaDetectada = true;
        }
        if (drone.ruido() > MAX_RUIDO) {
            violações.append(String.format("Ruído Ensurdecedor (%.2f); ", drone.ruido()));
            anomaliaDetectada = true;
        }

        if (anomaliaDetectada) {
            System.err.println("⛔ [PROXY FIREWALL] Bloqueando pacote malicioso de " + drone.posicao());
            throw new SecurityException("VALOR_ANOMALO: Conteúdo suspeito detectado -> " + violações.toString());
        }

        System.out.println("✅ [PROXY FIREWALL] Pacote de " + drone.posicao() + " aprovado. Encaminhando para LAN.");
    }
}