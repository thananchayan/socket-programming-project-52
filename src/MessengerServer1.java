
import java.io.*;
import java.net.*;
import java.util.*;
import java.io.Serializable;

public class MessengerServer1 {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 1234;
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
        private String username;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                String username = in.readLine();
                if (username != null && !username.isEmpty()) {
                    this.setUsername(username); // Set the username
                    System.out.println(username + " added to the chat");
                    sendToAllClients("SERVER~" + username + " added to the chat");
                } else {
                    System.out.println("Client username is empty");
                }
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("FILE:")) {
                        String fileName = message.substring(5);
                        String fileData = in.readLine();
                        byte[] decodedFileData = Base64.getDecoder().decode(fileData);
                        broadcastFile(username, fileName, decodedFileData);
                    } else if (message.startsWith("IMAGE:")) {
                        String imageName = message.substring(6);
                        String imageData = in.readLine();
                        byte[] decodedImageData = Base64.getDecoder().decode(imageData);
                        broadcastImage(username, imageName, decodedImageData);
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
                    sendToAllClients("SERVER~" + username + " left the chat");
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


private void broadcastFile(String username, String fileName, byte[] fileData) throws IOException {
    for (ClientHandler clientHandler : activeClients) {
        if (clientHandler != this) {
            clientHandler.sendMessage("FILE:" + username + "~" + fileName);
            clientHandler.sendMessage(Base64.getEncoder().encodeToString(fileData));
        }
    }
}



        public class DataObject implements Serializable {
            private String imageName;
            private byte[] imageData;

            public DataObject(String imageName, byte[] imageData) {
                this.imageName = imageName;
                this.imageData = imageData;
            }

            public String getImageName() {
                return imageName;
            }

            public byte[] getImageData() {
                return imageData;
            }
        }


        private void broadcastImage(String username, String imageName, byte[] imageData) throws IOException {
            DataObject dataObject = new DataObject(imageName, imageData);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(dataObject);
            oos.close();
            String base64DataObject = Base64.getEncoder().encodeToString(baos.toByteArray());
            for (ClientHandler clientHandler : activeClients) {
                if (clientHandler != this) {
                    clientHandler.sendMessage("IMAGE:" + username);
                    clientHandler.sendMessage(base64DataObject);
                }
            }
        }

    }
}
