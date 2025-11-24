package org.example.Interfaces;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class ListarInterfacesRede {
    public static void main(String[] args) throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        System.out.println("Interfaces de Rede Disponíveis:");
        while (interfaces.hasMoreElements()) {
            NetworkInterface net = interfaces.nextElement();
            System.out.println("Nome: " + net.getName());
            System.out.println("Display Name: " + net.getDisplayName());
            System.out.println("Up: " + net.isUp());
            System.out.println("Loopback: " + net.isLoopback());
            System.out.println("Multicast: " + net.supportsMulticast());
            System.out.println("---------------------------------------");
        }
    }
}
