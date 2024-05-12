import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

public class MessengerClient1 {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 1234;

    private static final Color DARK_BLUE = new Color(36, 52, 71);
    private static final Color LIGHT_BLUE = new Color(135, 206, 250);
    private static final Color LIGHT_GRAY = new Color(211, 211, 211);
    private static final Color WHITE = Color.WHITE;

    private static final Font FONT = new Font("Arial", Font.PLAIN, 17);
    private static final Font BUTTON_FONT = new Font("Arial", Font.BOLD, 15);
    private static final Font SMALL_FONT = new Font("Arial", Font.PLAIN, 13);

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame frame;
    private JTextArea messageBox;
    private JTextField messageTextField;
    private JTextField usernameTextField;
    private Image receivedImage;

    public MessengerClient1() {
        frame = new JFrame("Messenger Client");
        frame.setSize(870, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(DARK_BLUE);

        JPanel topPanel = new JPanel();
        topPanel.setBackground(DARK_BLUE);
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel usernameLabel = new JLabel("Enter your UserName: ");
        usernameLabel.setFont(FONT);
        usernameLabel.setForeground(LIGHT_BLUE);
        topPanel.add(usernameLabel);
        usernameTextField = new JTextField(20);
        usernameTextField.setFont(FONT);
        usernameTextField.setBackground(LIGHT_GRAY);
        usernameTextField.setForeground(DARK_BLUE);
        topPanel.add(usernameTextField);
        JButton joinButton = new JButton("Join");
        joinButton.setFont(BUTTON_FONT);
        joinButton.setBackground(LIGHT_BLUE);
        joinButton.setForeground(DARK_BLUE);
        joinButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connect();
            }
        });
        topPanel.add(joinButton);

        JPanel middlePanel = new JPanel();
        middlePanel.setBackground(DARK_BLUE);
        middlePanel.setLayout(new BorderLayout());
        messageBox = new JTextArea(25, 50);
        messageBox.setFont(SMALL_FONT);
        messageBox.setBackground(LIGHT_GRAY);
        messageBox.setForeground(DARK_BLUE);
        messageBox.setEditable(false);
        middlePanel.add(new JScrollPane(messageBox), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(DARK_BLUE);
        bottomPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        messageTextField = new JTextField(40);
        messageTextField.setFont(FONT);
        messageTextField.setBackground(LIGHT_GRAY);
        messageTextField.setForeground(DARK_BLUE);
        bottomPanel.add(messageTextField);
        JButton sendButton = new JButton("Send");
        sendButton.setFont(BUTTON_FONT);
        sendButton.setBackground(LIGHT_BLUE);
        sendButton.setForeground(DARK_BLUE);
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        bottomPanel.add(sendButton);

        JButton transferButton = new JButton("Transfer File or Image");
        transferButton.setFont(BUTTON_FONT);
        transferButton.setBackground(LIGHT_BLUE);
        transferButton.setForeground(DARK_BLUE);
        transferButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                transferFileOrImage();
            }
        });
        bottomPanel.add(transferButton);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(middlePanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void connect() {
        try {
            clientSocket = new Socket(HOST, PORT);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            System.out.println("Successfully connected to server");
            addMessage("[SERVER] Successfully connected to the server");
            String username = usernameTextField.getText();
            if (!username.isEmpty()) {
                out.println(username);
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid username", "Error", JOptionPane.ERROR_MESSAGE);
            }
            new Thread(new Runnable() {
                public void run() {
                    listenForMessagesFromServer();
                }
            }).start();
            usernameTextField.setEnabled(false);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Unable to connect to server " + HOST + " " + PORT, "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String message = messageTextField.getText();
        if (!message.isEmpty()) {
            out.println(message);
            messageTextField.setText("");
        } else {
            JOptionPane.showMessageDialog(frame, "Message cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void transferFileOrImage() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile != null) {
                    sendFile(selectedFile);
            }
        }
    }

    private void sendFile(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] fileData = new byte[(int) file.length()];
            fileInputStream.read(fileData);
            out.println("FILE:" + file.getName()); // Send the file name
            out.println(Base64.getEncoder().encodeToString(fileData)); // Send the file data
            fileInputStream.close();
            addMessage("[You] sent a file: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void addMessage(String message) {
        messageBox.append(message + "\n");
    }

    private void listenForMessagesFromServer() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("IMAGE:")) {
                    String username = message.substring(6);
                    String base64DataObject = in.readLine();
                    byte[] dataObjectBytes = Base64.getDecoder().decode(base64DataObject);
                    ByteArrayInputStream bais = new ByteArrayInputStream(dataObjectBytes);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    MessengerServer1.ClientHandler.DataObject dataObject = (MessengerServer1.ClientHandler.DataObject) ois.readObject();
                    ois.close();
                    receiveFile(username, dataObject.getImageName(), dataObject.getImageData());
                }  else if (message.startsWith("FILE:")) {
                    String[] parts = message.split("~");
                    String username = parts[0].substring(5);
                    String fileName = parts[1];
                    String fileDataString = in.readLine();
                    if (isBase64(fileDataString)) {
                        byte[] fileData = Base64.getDecoder().decode(fileDataString);
                        receiveFile(username, fileName, fileData);
                    } else {
                        System.out.println("Received file data is not Base64 encoded");
                    }
                }
                else {
                    addMessage(message);
                    // ...
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(frame, "Error: Message received from server is empty", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private boolean isBase64(String s) {
        return (s.length() % 4 == 0) && (s.matches("[A-Za-z0-9+/]+={0,2}"));
    }
    private void receiveFile(String username, String fileName, byte[] fileData) {
        try {
            // Determine the file extension
            String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : "";

            // Save the file data to a temporary file with the appropriate extension
            File tempFile = File.createTempFile("received", extension);
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            fileOutputStream.write(fileData);
            fileOutputStream.close();

            // Display the message in the message box
            addMessage(username + " sent a file: " + fileName);

            // Provide an option to open or save the file
            int choice = JOptionPane.showConfirmDialog(frame, "Do you want to open or save the file?", "File Received", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                try {
                    // Open the file
                    Desktop.getDesktop().open(tempFile);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(frame, "No application found to open this file. Please save the file and open it manually.", "Error Opening File", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                // Save the file
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(fileName));
                int result = fileChooser.showSaveDialog(frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    Files.copy(tempFile.toPath(), selectedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MessengerClient1();
            }
        });
    }
}
