import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static PrintWriter out;
    
    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);
        
        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // start a thread to listen for server messages
        ServerClientHandler listener = new ServerClientHandler();
        Thread t = new Thread(listener);
        t.start();

        System.out.print("Chat sessions has started - enter a user name: ");
        String name = userInput.nextLine().trim();
        out.println(name); //out.flush();

        String line = userInput.nextLine().trim();
        while(!line.toLowerCase().startsWith("/quit")) {
            String msg;
            if(line.startsWith("@")) {
                msg = String.format("PCHAT %s", line);
            }

            else if(line.startsWith("/rollDie")){
                msg = "Die Roll: " +  (int)(Math.random() * 6 + 1);

            }
            else if(line.startsWith("/flipCoin")){
                int temp = (int)Math.random() * 2;
                if(temp == 0){
                    msg = "Coin Flip: Heads";
                }
                else{
                    msg = "Coin Flip: Tails";
                }
            }


            else {
                msg = String.format("CHAT %s", line);
            }
            out.println(msg);
            line = userInput.nextLine().trim();
        }
        out.println("QUIT");
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
}
