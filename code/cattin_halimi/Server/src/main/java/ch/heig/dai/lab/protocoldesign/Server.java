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
    final String STOP = "STOP";

    final static String END_OF_MESSAGE = "\n";

    public static void main(String[] args) {
        // Create a new server and run it
        Server server = new Server();
        server.run();
    }

    /**
     * Principal method of the server, it listens on the port and accepts new connections.
     */
    private void run() {
        // Start the server and listen on the port
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Le serveur a démarré, il écoute le port " + SERVER_PORT);

            while (true) {
                // Accepte new connection
                try (Socket socket = serverSocket.accept();
                     var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8))) {
                    System.out.println("Nouvelle connection acceptée");

                    // Envoi du menu d'instructions au client
                    out.write(getMenu() + END_OF_MESSAGE);
                    out.flush();

                    while (true) {
                        // reads the message sent by the client and stops the loop if the message is null or STOP
                        String message = in.readLine();
                        if (message == null || message.equals(STOP)) {
                            break;
                        }


                        // Determines if the operation is known by the server, if not returns an error message
                        // and restarts the loop
                        String[] words = message.split(" ");
                        Operation operation = Operation.fromString(words[0]);

                        if (null == operation) {
                            out.write(UNKNOWN + " " + words[0] + END_OF_MESSAGE);
                            out.flush();
                            continue;
                        }


                        // Determins if the operation is possible with the arguments given by the client else
                        // returnes an error message and restarts the loop
                        int[] args = new int[words.length - 1];
                        try {
                            for (int i = 1; i < words.length; i++) {
                                args[i - 1] = Integer.parseInt(words[i]);
                            }
                        } catch (NumberFormatException e) {
                            out.write(BAD_DATA + "Problème de conversion en entier " + e + END_OF_MESSAGE);
                            out.flush();
                            continue;
                        }

                        String argumentError = operation.checkArguments(args);
                        if (!argumentError.isBlank()) {
                            out.write(BAD_DATA + " " + argumentError + END_OF_MESSAGE);
                            out.flush();
                            continue;
                        }

                        // Calcilates the result of the operation and sends it to the client
                        out.write(RESULT + " " + operation.calculate(args) + END_OF_MESSAGE);
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
     * Génère le menu des opérations disponibles en utilisant l'énumération Operation.
     * @return String contenant le menu
     */
    private String getMenu() {
        StringBuilder menu = new StringBuilder("Bienvenue sur la calculatrice serveur!\n");
        menu.append("Voici les opérations disponibles :\n");

        for (Operation op : Operation.values()) {
            menu.append("- ").append(op.getOperation()).append(" ");
            switch (op) {
                case ADD, SUB, MULT, DIV -> menu.append("x y ...     : ").append(op.getDescription()).append(" (plusieurs arguments)\n");
                case SQR,SQRT -> menu.append("x          : ").append(op.getDescription()).append(" (un seul argument)\n");
            }
        }
        menu.append("- STOP            : Arrêter la connexion\n");
        menu.append("END_MENU");  // Marker to indicate the end of the menu
        return menu.toString();
    }


    /**
     * Enumerates the different operations that can be performed on a list of integers.
     */
    enum Operation {
        ADD("ADD", "Addition"),
        SUB("SUB", "Soustraction"),
        MULT("MULT", "Multiplication"),
        DIV("DIV", "Division"),
        SQR("SQR", "Carré"),
        SQRT("SQRT", "Racine carrée");

        private final String operation;
        private final String description;

        /**
         * Constructor of the enum Operation with the name of the operation.
         *
         * @param operation the name of the operation.
         * @param description the description of the operation.
         */
        Operation(String operation, String description) {
            this.operation = operation;
            this.description = description;
        }

        /**
         * Return the operation corresponding to the name given in parameter.
         *
         * @param operation the name of the operation.
         * @return the operation corresponding to the name given in parameter, null if the operation is unknown.
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
         * Return the name of the operation as a string.
         *
         * @return the name of the operation as a string.
         */
        public String getOperation() {
            return operation;
        }

        /**
         * Return the description of the operation.
         *
         * @return the description of the operation.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Check if the arguments are correct for the operation.
         *
         * @param args list of integers on which the operation is performed.
         * @return an empty string if the arguments are correct, an error message otherwise.
         */
        public String checkArguments(int[] args) {
            return switch (this) {
                case ADD, SUB, MULT -> "";  // Aucune restriction
                case DIV -> {
                    for (int arg : args) {
                        if (arg == 0)
                            yield "division par 0";
                    }
                    yield "";
                }
                case SQR -> {
                    if (args.length > 1) {
                        yield "trop d'arguments pour SQR";
                    } else {
                        yield "";
                    }
                }
                case SQRT -> {
                    if (args.length > 1) {
                        yield "trop d'arguments pour SQRT";
                    } else if (args[0] < 0) {
                        yield "racine de nombre négatif";
                    } else {
                        yield "";
                    }
                }
            };
        }

        /**
         * Does the calculation of the specific operation on the list of integers.
         *
         * @param args list of integers on which the operation is performed.
         * @return the result of the operation.
         * @throws ArithmeticException if an error occurs during the calculation, if the arguments are
         *                    not verified correctly.
         */
        public int calculate(int[] args) throws ArithmeticException {
            return switch (this) {
                case ADD -> Arrays.stream(args).sum();
                case SUB -> Arrays.stream(args).reduce((a, b) -> a - b).orElse(0);
                case MULT -> Arrays.stream(args).reduce((a, b) -> a * b).orElse(1);
                case DIV -> Arrays.stream(args).reduce((a, b) -> a / b).orElse(1);
                case SQR -> args[0] * args[0];
                case SQRT -> (int) Math.sqrt(args[0]);
            };
        }
    }
}