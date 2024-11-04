package ch.heig.dai.lab.protocoldesign;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Server {
    final int SERVER_PORT = 1234;
    final String UNKNOWN = "UNKNOWN";
    final String BAD_DATA = "BAD_DATA";
    final String RESULT = "RESULT";

    public static void main(String[] args) {
        // Create a new server and run it
        Server server = new Server();
        server.run();
    }

    /**
     * Méthod principal qui définit les actions du serveur.
     */
    private void run() {
        // Démarrage du serveur
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Le serveur a démarré, il écoute le port " + SERVER_PORT);

            while (true) {
                // Acceptation de la connection
                try (Socket socket = serverSocket.accept();
                     var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
                     var out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8))) {
                    System.out.println("Nouvelle connection acceptée");

                    while (true) {
                        // Lis le message entré par le client, relance la boucle en cas d'erreur
                        String message = in.readLine();
                        if (null == message) break;

                        // Détermine l'opération demandée et renvoie une erreur si l'opération est
                        // inconnue et relance la boucle
                        String[] words = message.split(" ");
                        Operation operation = Operation.fromString(words[0]);
                        if (null == operation) {
                            out.write(UNKNOWN + " " + words[0]);
                            out.flush();
                            break;
                        }

                        // Détermine si l'opération est possible avec les arguments passés sinon
                        // renvoie une erreur et relance la boucle
                        int[] args = new int[words.length - 1];
                        try {
                            for (int i = 1; i < words.length; i++) {
                                args[i - 1] = Integer.parseInt(words[i]);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Problème de conversion en entier " + e);
                        }
                        String argumentError = operation.checkArguments(args);
                        if (!argumentError.isBlank()) {
                            out.write(BAD_DATA + " " + argumentError);
                            out.flush();
                            break;
                        }

                        // Calcule le résultat et le renvoie
                        out.write(RESULT + " " + operation.calculate(args));
                        out.flush();
                    }
                } catch (IOException e) {
                    System.out.println("Problème de connection client : " + e);
                }
            }
        } catch (IOException e) {
            System.out.println("Problème de connection serveur : " + e);
        }
    }

    /**
     * Enumère les opérations que le serveur peut effectuer.
     */
    enum Operation {
        ADD("ADD"),
        SUB("SUB"),
        MULT("MULT"),
        DIV("DIV"),
        SQRT("SQRT");

        private final String operation;

        /**
         * Constructeur d'une opération avec son nom.
         *
         * @param operation le nom de l'opération.
         */
        Operation(String operation) {
            this.operation = operation;
        }

        /**
         * Retourne l'opération correspondant à un nom.
         *
         * @param operation le nom de l'opération.
         * @return l'opération correspondant à un nom, null si aucune opéraiton n'est liée à ce nom.
         */
        public static Operation fromString(String operation) {
            for (Operation op : Operation.values()) {
                if (op.getOperation().equals(operation)) {
                    return op;
                }
            }
            return null;
        }

        /**
         * Retourne le nom de l'opération sous forme de String.
         *
         * @return le nom de l'opération sous forme de String.
         */
        public String getOperation() {
            return operation;
        }

        /**
         * Vérifier si une liste d'arguments entiers est compatible avec une opération spécifique.
         *
         * @param args la liste d'arguments entiers.
         * @return un String vide si l'opération est possible, un message d'erreur sinon.
         */
        public String checkArguments(int[] args) {
            return switch (this) {
                case ADD, SUB, MULT -> "";
                case DIV -> {
                    for (int arg : args) {
                        if (0 == arg)
                            yield "division par 0";
                    }
                    yield "";
                }
                case SQRT -> {
                    if (1 < args.length) {
                        yield "trop d'argmuents";
                    } else if (0 > args[0]) {
                        yield "racine de nombre négatif";
                    } else {
                        yield "";
                    }
                }
            };
        }

        /**
         * Effectue un calcul sur une liste d'entiers suivant une opération spécifique.
         *
         * @param args la liste d'entiers sur laquelle effectuer l'opération.
         * @return le résultat du calcul.
         * @throws ArithmeticException si une erreur a lieu, notamment si les arguments n'ont pas
         *                             été vérifiés correctement.
         */
        public int calculate(int[] args) throws ArithmeticException {
            return switch (this) {
                case ADD -> Arrays.stream(args).sum();
                case SUB -> Arrays.stream(args).reduce((a, b) -> a - b).orElse(0);
                case MULT -> Arrays.stream(args).reduce((a, b) -> a * b).orElse(1);
                case DIV -> Arrays.stream(args).reduce((a, b) -> a / b).orElse(1);
                case SQRT -> (int) Math.sqrt(args[0]);
            };
        }
    }
}