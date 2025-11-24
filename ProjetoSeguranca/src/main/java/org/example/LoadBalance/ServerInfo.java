package org.example.LoadBalance;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Objects;

public class ServerInfo {
    private final String id;
    private final String host;
    private final int porta;
    private final AtomicInteger conexoesAtivas = new AtomicInteger(0); // Inicializado diretamente
    private final int peso;

    public ServerInfo(String id, String host, int porta, int peso) {
        this.id = id;
        this.host = host;
        this.porta = porta;
        this.peso = peso;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPorta() {
        return porta;
    }

    public int getPeso() {
        return peso;
    }

    public int getConexoesAtivas() {
        return conexoesAtivas.get();
    }

    public void setConexoesAtivas(int count) {
        if (count >= 0) {
            conexoesAtivas.set(count);
        } else {
            System.err.println("Tentativa de definir contagem de conexões negativa para " + id + ": " + count + ". Definindo para 0.");
            conexoesAtivas.set(0);
        }
    }

    public void incrementarConexoes() {
        conexoesAtivas.incrementAndGet();
    }

    public void decrementarConexoes() {
        // Garante que não fique negativo
        conexoesAtivas.getAndUpdate(current -> Math.max(0, current - 1));
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "id='" + id + '\'' +
                ", host='" + host + '\'' +
                ", porta=" + porta +
                ", conexoesAtivas=" + conexoesAtivas.get() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServerInfo)) return false;
        ServerInfo that = (ServerInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
