import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.*;

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
            String filePath = selectedFile.getAbsolutePath();
            out.println("FILE:" + filePath);
        }
    }

    private void sendImage() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String imagePath = selectedFile.getAbsolutePath();
            out.println("IMAGE:" + imagePath);
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
                    addMessage(username + ": " + fileName);
                } else if (message.startsWith("IMAGE:")) {
                    String[] parts = message.split("~");
                    String username = parts[0].substring(6);
                    String imagePath = parts[1];
                    addMessage(username + ": " + imagePath);
                } else if (message.equals("SAVE_FILE")) {
                    saveFile();
                } else if (message.equals("SAVE_IMAGE")) {
                    saveImage();
                } else {
                    addMessage(message);
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error: Message received from server is empty", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                FileOutputStream fos = new FileOutputStream(fileToSave);
                InputStream is = clientSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.close();
                JOptionPane.showMessageDialog(frame, "File saved successfully", "File Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void receiveImage(String username, String imagePath) {
        try {
            // Load the image from the specified path
            receivedImage = ImageIO.read(new File(imagePath));

            // Display the image in a JOptionPane dialog
            ImageIcon imageIcon = new ImageIcon(receivedImage);
            JLabel label = new JLabel(imageIcon);
            JOptionPane.showMessageDialog(frame, label, "Image received from " + username, JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveImage() {
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


    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new MessengerClient1();
            }
        });
    }
}
