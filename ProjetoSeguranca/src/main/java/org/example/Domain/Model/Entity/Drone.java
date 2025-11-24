package org.example.Domain.Model.Entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Drone implements Serializable {

    private static int count = 0;
    private int id;

    // Novos campos de poluentes e partículas
    private double co2;
    private double co;
    private double no2;
    private double so2;
    private double pm2_5;
    private double pm10;

    // Campos existentes
    private double temperatura;
    private double umidade;
    private double ruido;
    private double radiacao;
    private double latitude;
    private double longitude;
    private String posicao;
    private LocalDate dataCriacao;


    public Drone(double co2, double co, double no2, double so2,
                 double pm2_5, double pm10, double temperatura, double umidade,
                 double ruido, double radiacao, double latitude, double longitude,
                 String posicao) {

        setId(getNextId());

        setCo2(co2);
        setCo(co);
        setNo2(no2);
        setSo2(so2);
        setPm2_5(pm2_5);
        setPm10(pm10);

        setTemperatura(temperatura);
        setUmidade(umidade);
        setRuido(ruido);
        setRadiacao(radiacao);
        setLatitude(latitude);
        setLongitude(longitude);
        setPosicao(posicao);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        setDataCriacao(LocalDate.parse(formatter.format(LocalDate.now()), formatter));

    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getNextId() {
        return ++count;
    }

    public String getPosicao() {
        return posicao;
    }

    public void setPosicao(String posicao) {
        this.posicao = posicao;
    }

    public double getCo2() {
        return co2;
    }

    public void setCo2(double co2) {
        this.co2 = co2;
    }

    public double getCo() {
        return co;
    }

    public void setCo(double co) {
        this.co = co;
    }

    public double getNo2() {
        return no2;
    }

    public void setNo2(double no2) {
        this.no2 = no2;
    }

    public double getSo2() {
        return so2;
    }

    public void setSo2(double so2) {
        this.so2 = so2;
    }

    public double getPm2_5() {
        return pm2_5;
    }

    public void setPm2_5(double pm2_5) {
        this.pm2_5 = pm2_5;
    }

    public double getPm10() {
        return pm10;
    }

    public void setPm10(double pm10) {
        this.pm10 = pm10;
    }

    public double getRuido() {
        return ruido;
    }

    public void setRuido(double ruido) {
        this.ruido = ruido;
    }

    public double getRadiacao() {
        return radiacao;
    }

    public void setRadiacao(double radiacao) {
        this.radiacao = radiacao;
    }

    public double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(double temperatura) {
        this.temperatura = temperatura;
    }

    public double getUmidade() {
        return umidade;
    }

    public void setUmidade(double umidade) {
        this.umidade = umidade;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public LocalDate getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDate dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    @Override
    public String toString() {
        return "Drone{" +
                "id=" + id +
                ", co2=" + co2 +
                ", co=" + co +
                ", no2=" + no2 +
                ", so2=" + so2 +
                ", pm2_5=" + pm2_5 +
                ", pm10=" + pm10 +
                ", temperatura=" + temperatura +
                ", umidade=" + umidade +
                ", ruido=" + ruido +
                ", radiacao=" + radiacao +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", posicao='" + posicao + '\'' +
                ", dataCriacao='" + dataCriacao + '\'' +
                '}';
    }
}