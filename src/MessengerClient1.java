// MessengerClient1.java

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
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
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(DARK_BLUE);

        JPanel topPanel = new JPanel();
        topPanel.setBackground(DARK_BLUE);
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel usernameLabel = new JLabel("Enter your UserName for Group chat: ");
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

        JButton fileButton = new JButton("Send File");
        fileButton.setFont(BUTTON_FONT);
        fileButton.setBackground(LIGHT_BLUE);
        fileButton.setForeground(DARK_BLUE);
        fileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });
        bottomPanel.add(fileButton);

        JButton imageButton = new JButton("Send Image");
        imageButton.setFont(BUTTON_FONT);
        imageButton.setBackground(LIGHT_BLUE);
        imageButton.setForeground(DARK_BLUE);
        imageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendImage();
            }
        });
        bottomPanel.add(imageButton);

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

private void sendFile() {
    JFileChooser fileChooser = new JFileChooser();
    int result = fileChooser.showOpenDialog(frame);
    if (result == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        try {
            FileInputStream fileInputStream = new FileInputStream(selectedFile);
            byte[] fileData = new byte[(int) selectedFile.length()];
            fileInputStream.read(fileData);
            out.println("FILE:" + selectedFile.getName()); // Send the file name
            out.println(Base64.getEncoder().encodeToString(fileData)); // Send the file data
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

private void sendImage() {
    JFileChooser fileChooser = new JFileChooser();
    int result = fileChooser.showOpenDialog(frame);
    if (result == JFileChooser.APPROVE_OPTION) {
        File selectedFile = fileChooser.getSelectedFile();
        try {
            BufferedImage bufferedImage = ImageIO.read(selectedFile);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
            byte[] imageData = byteArrayOutputStream.toByteArray();
            out.println("IMAGE:" + selectedFile.getName()); // Send the image name
            out.println(Base64.getEncoder().encodeToString(imageData)); // Send the image data
            byteArrayOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
    private void addMessage(String message) {
        messageBox.append(message + "\n");
    }

    private void listenForMessagesFromServer() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("FILE:")) {
                    String[] parts = message.split("~");
                    String username = parts[0].substring(5);
                    String fileName = parts[1];
                    byte[] fileData = Base64.getDecoder().decode(in.readLine());
                    receiveFile(username, fileName, fileData);
                } else if (message.startsWith("IMAGE:")) {
                    String[] parts = message.split("~");
                    String username = parts[0].substring(6);
                    String imageName = parts[1];
                    byte[] imageData = Base64.getDecoder().decode(in.readLine());
                    receiveImage(username, imageName, imageData);
                } else {
                    addMessage(message);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error: Message received from server is empty", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void receiveFile(String username, String fileName, byte[] fileData) {
        try {
            // Save the file data to a temporary file
            File tempFile = File.createTempFile("received", null);
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            fileOutputStream.write(fileData);
            fileOutputStream.close();

            // Display the message in the message box
            addMessage(username + " sent a file: " + fileName);

            // Provide an option to open or save the file
            int choice = JOptionPane.showConfirmDialog(frame, "Do you want to open or save the file?", "File Received", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                // Open the file
                Desktop.getDesktop().open(tempFile);
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

    private void receiveImage(String username, String imageName, byte[] imageData) {
        try {
            // Decode the Base64-encoded image data
            byte[] decodedImageData = Base64.getDecoder().decode(imageData);

            // Convert the decoded byte array to an Image
            Image receivedImage = ImageIO.read(new ByteArrayInputStream(decodedImageData));

            // Scale the image to fit within the chat window
            int width = receivedImage.getWidth(null);
            int height = receivedImage.getHeight(null);
            int scaledWidth = width > 200 ? 200 : width; // Limit width to 200 pixels
            int scaledHeight = height * scaledWidth / width; // Maintain aspect ratio
            Image scaledImage = receivedImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);

            // Convert the scaled image to a Base64-encoded string
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write((BufferedImage) scaledImage, "png", baos);
            String encodedImage = Base64.getEncoder().encodeToString(baos.toByteArray());

            // Construct HTML content to display the image
            String htmlContent = "<html><b>" + username + " sent an image:</b><br>" +
                    "<img src='data:image/png;base64," + encodedImage + "'/></html>";

            // Append the HTML content to the messageBox JTextArea
            messageBox.setText(messageBox.getText() + htmlContent + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void saveImage(byte[] imageData) {
        // Implement the logic to save the received image
        if (receivedImage != null) {
            // Use receivedImage to save the image
            // For example, you can use ImageIO.write() to save the image to a file
            // This example assumes that the received image is in a format supported by ImageIO
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                try {
                    ImageIO.write((RenderedImage) receivedImage, "png", fileToSave); // Change "png" to the appropriate image format
                    JOptionPane.showMessageDialog(frame, "Image saved successfully", "Image Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(frame, "Error saving image: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame, "No image received", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
//    private void receiveImage(String username, String imageName, byte[] imageData) {
//        try {
//            // Decode the image data and display it in a dialog
//            ImageIcon imageIcon = new ImageIcon(imageData);
//            JLabel label = new JLabel(imageIcon);
//            JOptionPane.showMessageDialog(frame, label, "Image received from " + username, JOptionPane.PLAIN_MESSAGE);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MessengerClient1();
            }
        });
    }
}
