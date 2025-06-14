import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField, recipientField;
    private JButton sendButton, endButton;
    private DataOutputStream dos;
    private DataInputStream dis;
    private Socket socket;

    public ChatClientGUI(String serverIP, int port) {
        try {
            socket = new Socket(serverIP, port);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            String username = JOptionPane.showInputDialog("Enter your name:");
            if (username == null || username.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Username cannot be empty.");
                System.exit(0);
            }

            dos.writeUTF(username.trim());
            buildGUI(username.trim());
            listenForMessages();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Connection failed: " + e.getMessage());
            System.exit(0);
        }
    }

    private void buildGUI(String username) {
        frame = new JFrame("LAN Chat - " + username);
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                endChat();
            }
        });

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        recipientField = new JTextField();
        recipientField.setToolTipText("Recipient username");
        recipientField.setPreferredSize(new Dimension(120, 30));

        inputField = new JTextField();
        inputField.setToolTipText("Enter message here");

        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        endButton = new JButton("End Chat");
        endButton.addActionListener(e -> endChat());

        inputField.addActionListener(e -> sendMessage());

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(recipientField, BorderLayout.WEST);
        bottomPanel.add(inputField, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        rightPanel.add(sendButton);
        rightPanel.add(endButton);

        bottomPanel.add(rightPanel, BorderLayout.EAST);

        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private void sendMessage() {
        try {
            String recipient = recipientField.getText().trim();
            String message = inputField.getText().trim();
            if (!recipient.isEmpty() && !message.isEmpty()) {
                dos.writeUTF(recipient);
                dos.writeUTF(message);
                chatArea.append("Me → " + recipient + ": " + message + "\n");
                inputField.setText("");
            }
        } catch (IOException e) {
            chatArea.append("Error sending message.\n");
        }
    }

    private void listenForMessages() {
        Thread readThread = new Thread(() -> {
            try {
                while (true) {
                    String msg = dis.readUTF();
                    chatArea.append(msg + "\n");
                }
            } catch (IOException e) {
                chatArea.append("Disconnected from server.\n");
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    private void endChat() {
        try {
            dos.writeUTF("**EXIT**");
        } catch (IOException ignored) {}
        try {
            socket.close();
            dis.close();
            dos.close();
        } catch (IOException ignored) {}
        frame.dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        String ip = JOptionPane.showInputDialog("Enter Server IP:");
        if (ip == null || ip.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "IP cannot be empty.");
            return;
        }
        new ChatClientGUI(ip.trim(), 1234);
    }
}
