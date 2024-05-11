import java.io.*;
import java.net.*;
import java.util.*;

public class MessengerServer1 {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 1234;
    private static final int LISTENER_LIMIT = 5;
    private static List<ClientHandler> activeClients = new ArrayList<>();

    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Running the server on " + HOST + " " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Successfully connected to client " + clientSocket.getInetAddress() + " " + clientSocket.getPort());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                activeClients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Unable to bind to host " + HOST + " and port " + PORT);
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String username = in.readLine();
                if (username != null && !username.isEmpty()) {
                    System.out.println(username + " added to the chat");
                    sendToAllClients("SERVER~" + username + " added to the chat");
                } else {
                    System.out.println("Client username is empty");
                }
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("FILE:")) {
                        String fileName = message.substring(5);
                        broadcastFile(username, fileName);
                    } else if (message.startsWith("IMAGE:")) {
                        String imagePath = message.substring(6);
                        broadcastImage(username, imagePath);
                    } else {
                        sendToAllClients(username + "~" + message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    clientSocket.close();
                    activeClients.remove(this);
                    System.out.println("Client disconnected");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendToAllClients(String message) {
            for (ClientHandler clientHandler : activeClients) {
                clientHandler.sendMessage(message);
            }
        }

        private void sendMessage(String message) {
            out.println(message);
        }

        private void broadcastFile(String username, String fileName) throws IOException {
            String fileMessage = "FILE:" + username + "~" + fileName;
            for (ClientHandler clientHandler : activeClients) {
                clientHandler.sendMessage(fileMessage);
                clientHandler.sendMessage("SAVE_FILE"); // Send a separate message for save option
            }
        }

        private void broadcastImage(String username, String imagePath) throws IOException {
            String imageMessage = "IMAGE:" + username + "~" + imagePath;
            for (ClientHandler clientHandler : activeClients) {
                clientHandler.sendMessage(imageMessage);
                clientHandler.sendMessage("SAVE_IMAGE"); // Send a separate message for save option
            }
        }
    }
}
