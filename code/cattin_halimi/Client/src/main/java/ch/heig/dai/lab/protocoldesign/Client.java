package ch.heig.dai.lab.protocoldesign;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class Client {
    final String SERVER_ADDRESS = "127.0.0.1"; // Localhost
    final int SERVER_PORT = 1234;
    final static int TIMEOUT = 10000;

    final static String END_OF_MESSAGE = "\n";


    public static void main(String[] args) {
        // Create a new client and run it
        Client client = new Client();
        client.run();
    }

    private void run() {
        System.out.println("Ouverture de la connexion au serveur " + SERVER_ADDRESS + ":" + SERVER_PORT);

        try (var socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             var out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
             var consoleIn = new BufferedReader(new InputStreamReader(System.in))) {

            String userInput;

            while (true) {
                System.out.print("> ");
                userInput = consoleIn.readLine();

                // Envoie "STOP" au serveur pour terminer la connexion
                if (userInput.equalsIgnoreCase("STOP")) {
                    out.write("STOP" + END_OF_MESSAGE);
                    out.flush();
                    System.out.println("Disconnecting from server...");
                    break;
                }

                // Envoie l'entrée de l'utilisateur au serveur
                out.write(userInput + END_OF_MESSAGE);
                out.flush();

                // lit la réponse du serveur
                String serverResponse = in.readLine();
                if (serverResponse == null) {
                    System.out.println("Erreur : la connexion avec le serveur a été fermée.");
                    break;
                }

                // gérer la réponse du serveur en fonction du protocole
                handleServerResponse(serverResponse);
            }

        } catch (IOException e) {
            System.out.println("Erreur lors de la connexion au serveur: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Gère la réponse du serveur en fonction du protocole.
     *
     * @param response Réponse du serveur
     */
    private void handleServerResponse(String response) {
        if (response.startsWith("RESULT")) {
            System.out.println("Result: " + response.substring("RESULT ".length()));
        } else if (response.startsWith("UNKNOWN")) {
            System.out.println("Error: Unknown operation - " + response.substring("UNKNOWN ".length()));
        } else if (response.startsWith("BAD_DATA")) {
            System.out.println("Error: Invalid data - " + response.substring("BAD_DATA ".length()));
        } else {
            System.out.println("Unexpected response from server: " + response);
        }
    }
}