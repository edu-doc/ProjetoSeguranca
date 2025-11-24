package org.example.API.DTO;

public record DroneDTO(double co2, double co, double no2, double so2,
                       double pm2_5, double pm10, double temperatura, double umidade,
                       double ruido, double radiacao, double latitude, double longitude,
                       String posicao) {}
