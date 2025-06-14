import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 1234;
    private static final Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private DataInputStream dis;
        private DataOutputStream dos;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                dis = new DataInputStream(socket.getInputStream());
                dos = new DataOutputStream(socket.getOutputStream());

                username = dis.readUTF();
                synchronized (clients) {
                    if (clients.containsKey(username)) {
                        dos.writeUTF("Username already taken.");
                        socket.close();
                        return;
                    }
                    clients.put(username, this);
                    System.out.println(username + " connected.");
                }

                while (true) {
                    String recipient = dis.readUTF();
                    if (recipient.equals("**EXIT**")) break;
                    String message = dis.readUTF();

                    ClientHandler target = clients.get(recipient);
                    if (target != null) {
                        target.dos.writeUTF(username + " → You: " + message);
                    } else {
                        dos.writeUTF("User '" + recipient + "' not found.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Connection with " + username + " lost.");
            } finally {
                try {
                    socket.close();
                    dis.close();
                    dos.close();
                } catch (Exception ignored) {}
                synchronized (clients) {
                    clients.remove(username);
                    System.out.println(username + " disconnected.");
                }
            }
        }
    }
}
