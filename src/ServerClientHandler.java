import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

public class ServerClientHandler implements Runnable{
    // Inner class
    // Maintain data about the client serviced by this thread
    ClientConnectionData client;
    ArrayList<ClientConnectionData> clientList;

    public ServerClientHandler(ClientConnectionData client, ArrayList<ClientConnectionData> clientList) {
        this.client = client;
        this.clientList = clientList;
    }

    /**
     * Broadcasts a message to all clients connected to the server.
     */
    public void broadcast(String msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){
                    c.getOut().println(msg);
                    // c.getOut().flush();
                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }

    public void privateBroadcast(String msg, ClientConnectionData c) {
        try {
            System.out.println("Private Broadcasting to " + c.getUserName() + " -- " + msg);
            c.getOut().println(msg);
        }
        catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader in = client.getInput();
            //get userName, first message from user
            String userName = in.readLine().trim();
            client.setUserName(userName);
            //notify all that client has joined
            broadcast(String.format("WELCOME %s", client.getUserName()));


            String incoming = "";

            while( (incoming = in.readLine()) != null) {
                if (incoming.startsWith("CHAT")) {
                    String chat = incoming.substring(4).trim();
                    if (chat.length() > 0) {
                        String msg = String.format("CHAT %s %s", client.getUserName(), chat);
                        broadcast(msg);
                    }
                }

                else if(incoming.startsWith("PCHAT")) { // Start new
                    String chat = incoming.substring(7).trim();
                    int endOfUsernameIndex = chat.indexOf(" ");
                    String pchatUsername = chat.substring(0, endOfUsernameIndex).trim();
                    for (ClientConnectionData c: clientList) {
                        if(pchatUsername.equals(c.getUserName())) {
                            if(chat.substring(endOfUsernameIndex).trim().length() > 0) {
                                String receiverMSG = String.format("PCHAT %s %s", client.getUserName(), chat.substring(endOfUsernameIndex).trim());
                                String senderMSG = String.format("PCHAT %s %s", c.getUserName(), chat.substring(endOfUsernameIndex).trim());
                                privateBroadcast(receiverMSG, c);
                                privateBroadcast(senderMSG, client);
                            }
                        }
                    }
                }

                else if (incoming.startsWith("QUIT")){
                    break;
                }
            }
        } catch (Exception ex) {
            if (ex instanceof SocketException) {
                System.out.println("Caught socket ex for " +
                        client.getName());
            } else {
                System.out.println(ex);
                ex.printStackTrace();
            }
        } finally {
            //Remove client from clientList, notify all
            synchronized (clientList) {
                clientList.remove(client);
            }
            System.out.println(client.getName() + " has left.");
            broadcast(String.format("EXIT %s", client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ex) {}

        }
    }

}
