import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static ObjectOutputStream out;

    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);

        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new ObjectOutputStream(socket.getOutputStream());

        // start a thread to listen for server messages
        ClientServerHandler listener = new ClientServerHandler(socketIn);
        Thread t = new Thread(listener);
        t.start();

        System.out.print("Chat sessions has started - enter a user name: ");
        String name = userInput.nextLine().trim();
        out.writeObject(name); //out.flush();

        String line = userInput.nextLine().trim();
        while (!line.toLowerCase().startsWith("/quit")) {
            Serialization m;
            if (line.startsWith("@")) {
                m = new Serialization(Serialization.MSG_HEADER_PRIVATECHAT, line);
            } else if(line.startsWith("Die Roll:")) {
                m = new Serialization(Serialization.MSG_HEADER_DIEROLL, line);
            } else if(line.startsWith("Coin Flip:"))  {
                m = new Serialization(Serialization.MSG_HEADER_COINFLIP, line);
            } else if(line.startsWith("WhoIsHere")) {
                m = new Serialization(Serialization.MSG_HEADER_WHOISHERE, line);
            } else {
                m = new Serialization(Serialization.MSG_HEADER_CHAT, line);
            }
            out.writeObject(m);
            System.out.println(m);
            line = userInput.nextLine().trim();
        }
        //System.out.println();
        out.writeObject(new Serialization(Serialization.MSG_HEADER_QUIT, ""));
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
    }
}