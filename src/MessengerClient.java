import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class MessengerClient {

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

    public MessengerClient() {
        frame = new JFrame("Messenger Client");
        frame.setSize(650, 600);
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

    private void addMessage(String message) {
        messageBox.append(message + "\n");
    }

    private void listenForMessagesFromServer() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split("~");
                String username = parts[0];
                String content = parts[1];
                addMessage("[" + username + "] " + content);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Error: Message received from client is empty", "Error", JOptionPane.ERROR_MESSAGE);
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
