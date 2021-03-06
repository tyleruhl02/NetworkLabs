import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    //public static final int PORT = 54321;
    // IP Address: ec2-13-58-169-82.us-east-2.compute.amazonaws.com
    public static final int PORT = 59005;
    private static final ArrayList<ClientConnectionData> clientList = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(100);

        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Chat Server started.");
            System.out.println("Local IP: "
                    + Inet4Address.getLocalHost().getHostAddress());
            System.out.println("Local Port: " + serverSocket.getLocalPort());

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Connected to %s:%d on local port %d\n",
                            socket.getInetAddress(), socket.getPort(), socket.getLocalPort());

                    // This code should really be done in the separate thread
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    String name = socket.getInetAddress().getHostName();


                    ClientConnectionData client = new ClientConnectionData(socket, in, out, name);
                    synchronized (clientList) {
                        clientList.add(client);
                    }

                    System.out.println("added client " + name);

                    //handle client business in another thread
                    pool.execute(new ServerClientHandler(client, clientList));
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

            }
        }
    }
}