package org.example.Conexoes.Terceiras;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Arrays;

public class EmissorLauncher {

    private static final List<String> EMISSORES = Arrays.asList(
            "org.example.Conexoes.Terceiras.EmissorMultiCastNorte",
            "org.example.Conexoes.Terceiras.EmissorMultiCastSul",
            "org.example.Conexoes.Terceiras.EmissorMultiCastLeste",
            "org.example.Conexoes.Terceiras.EmissorMultiCastOeste"
    );

    // Lista para armazenar as referências dos processos filhos
    private static final List<Process> processosEmissores = new ArrayList<>();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================================");
        System.out.println("=== INICIADOR DE EMISSORES ===");
        System.out.println("=================================================");

        String pReceptor = "10423697762202072474192169435750446330735890693531947355900978750707473800292495315432137113961338858063603855560250612045484619910040503489589658312941539";

        String gReceptor = "2";

        String yReceptor = "5976663729856370060681444225294983634804753623565438066085334769300573191366560804362234458451536318299225001107040031416456779399275778240007454837028901";

        String classpath = System.getProperty("java.class.path");
        String javaCommand = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

        if (classpath == null || classpath.isEmpty()) {
            System.err.println("\nERRO: Falha ao determinar o classpath. Tente rodar via Maven ou IDE.");
            return;
        }

        System.out.println("\n--- INICIANDO PROCESSOS DOS EMISSORES ---");

        // --- 2. INICIAR OS 4 PROCESSOS INDEPENDENTES ---
        for (String emissorClass : EMISSORES) {
            try {
                // Monta o comando completo: java -cp <classpath> <classe> <arg P> <arg G> <arg Y>
                ProcessBuilder pb = new ProcessBuilder(
                        javaCommand,
                        "-cp", classpath,
                        emissorClass,
                        pReceptor,
                        gReceptor,
                        yReceptor
                );

                // Configura para que a saída e erro do processo filho apareçam no console do Launcher
                pb.inheritIO();

                Process p = pb.start();
                processosEmissores.add(p); // Armazena a referência para gerenciamento
                System.out.println("✅ Processo iniciado: " + emissorClass);

            } catch (IOException e) {
                System.err.println("❌ Erro ao iniciar processo " + emissorClass + ": " + e.getMessage());
                System.err.println("Verifique se o comando 'java' e o classpath estão corretos.");
            }
        }

        // --- 3. MONITORAMENTO E ENCERRAMENTO ---
        System.out.println("\n=================================================");
        System.out.println("Todos os " + processosEmissores.size() + " emissores estão enviando dados...");
        System.out.println("Pressione ENTER para encerrar todos os emissores.");

        // Espera a entrada do usuário para encerrar
        scanner.nextLine();

        System.out.println("--- ENCERRANDO PROCESSOS... ---");

        int encerrados = 0;
        for (Process p : processosEmissores) {
            if (p.isAlive()) {
                p.destroyForcibly(); // Encerra o processo de forma forçada
                encerrados++;
            }
        }

        System.out.println("=================================================");
        System.out.println("✅ " + encerrados + " processos de Emissor encerrados.");
        System.out.println("O Launcher foi finalizado.");
        scanner.close();
    }
}