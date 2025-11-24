package org.example.Domain.Service;

import org.example.Domain.Model.Entity.Drone;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServidorService {
    private final List<Drone> drones;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ServidorService() {
        this.drones = new ArrayList<>();
    }

    // --- MÉTODOS DE MANIPULAÇÃO DE DADOS ---
    public void addDrone(Drone drone) {
        drones.add(drone);
    }

    public List<Drone> getAllDrones() {
        return new ArrayList<>(drones);
    }

    // --- MÉTODOS DE BUSCA BÁSICA ---

    public List<Drone> getDronePosicao(String posicao) {
        return drones.stream()
                .filter(drone -> drone.getPosicao().equals(posicao))
                .collect(Collectors.toList());
    }

    public List<Drone> getDroneData(String data) {
        try {
            LocalDate dataBusca = LocalDate.parse(data, formatter);
            return drones.stream()
                    .filter(drone -> drone.getDataCriacao().equals(dataBusca))
                    .collect(Collectors.toList());
        } catch (DateTimeParseException e) {
            System.err.println("Erro ao processar data: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Drone> getDronePosicaoData(String posicao, String data) {
        try {
            LocalDate dataBusca = LocalDate.parse(data, formatter);
            System.out.println("Buscando drones na posição " + posicao + " e data " + dataBusca);

            List<Drone> resultado = drones.stream()
                    .filter(drone -> {
                        boolean posicaoMatch = drone.getPosicao().equals(posicao);
                        boolean dataMatch = drone.getDataCriacao().equals(dataBusca);
                        return posicaoMatch && dataMatch;
                    })
                    .collect(Collectors.toList());

            System.out.println("Total de drones encontrados: " + resultado.size());
            return resultado;
        } catch (DateTimeParseException e) {
            System.err.println("Erro ao processar data: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // --- NOVOS MÉTODOS DE ANÁLISE (RELATÓRIOS/IA) ---

    /**
     * Relatório 1: Calcula a média de CO2 e PM2.5 por posição.
     */
    public Map<String, String> getMediaPoluentesPorPosicao() {
        return drones.stream()
                .collect(Collectors.groupingBy(Drone::getPosicao,
                        Collectors.teeing(
                                Collectors.averagingDouble(Drone::getCo2),
                                Collectors.averagingDouble(Drone::getPm2_5),
                                (avgCo2, avgPm25) -> String.format("CO2: %.2f ppm, PM2.5: %.2f µg/m³", avgCo2, avgPm25)
                        )
                ));
    }

    /**
     * Relatório 2: Simula um Alerta de Tendência de Aumento de Temperatura (IA).
     */
    public String getAlertaTendenciaTemperatura() {
        if (drones.isEmpty()) return "Nenhum dado para análise.";

        // Média geral para comparação
        double mediaGeral = drones.stream().mapToDouble(Drone::getTemperatura).average().orElse(0.0);

        // Média dos últimos 25% dos dados como "dados recentes"
        int startIndex = (int) (drones.size() * 0.75);
        double mediaRecente = drones.subList(startIndex, drones.size()).stream()
                .mapToDouble(Drone::getTemperatura)
                .average()
                .orElse(0.0);

        // Se a temperatura recente for 2.0°C maior que a geral
        if (mediaRecente > mediaGeral + 2.0) {
            return "ALERTA (IA): Tendência de aumento de temperatura! Média recente (Últimos dados): " + String.format("%.1f°C", mediaRecente) + ". Média geral: " + String.format("%.1f°C", mediaGeral) + ".";
        } else {
            return "NORMAL: Temperatura estável. Média recente: " + String.format("%.1f°C", mediaRecente) + ".";
        }
    }

    /**
     * Relatório 3: Lista pontos (posições) onde o ruído ultrapassou um limite.
     */
    public List<String> getPontosCriticosRuido(double limiteRuido) {
        return drones.stream()
                .filter(drone -> drone.getRuido() > limiteRuido)
                .map(drone -> String.format("Posição: %s | Ruído: %.1f dB | Lat/Long: %.4f/%.4f",
                        drone.getPosicao(), drone.getRuido(), drone.getLatitude(), drone.getLongitude()))
                .distinct() // Remove duplicatas
                .collect(Collectors.toList());
    }

    /**
     * Relatório 4: Previsão Simples da Qualidade do Ar (IA).
     */
    public String getPrevisaoQualidadeAr() {
        long contagemRuim = drones.stream().filter(drone ->
                drone.getCo() > 10.0 ||
                        drone.getNo2() > 0.2 ||
                        drone.getPm2_5() > 30.0
        ).count();

        if (drones.isEmpty()) return "Nenhum dado para previsão.";

        if (contagemRuim > drones.size() * 0.2) { // Mais de 20% das amostras com problemas
            return "PREVISÃO (IA): Qualidade do ar será RUIM. Tendência de alta concentração de poluentes (Total de amostras ruins: " + contagemRuim + ").";
        } else {
            return "PREVISÃO (IA): Qualidade do ar será BOA. Baixa concentração de poluentes.";
        }
    }

    /**
     * Relatório 5: Contagem de entradas de dados por posição.
     */
    public Map<String, Long> getContagemPorPosicao() {
        return drones.stream()
                .collect(Collectors.groupingBy(Drone::getPosicao, Collectors.counting()));
    }
}