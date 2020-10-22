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
            Boolean validUserName = false;

            // checks that username is unique

            while(!validUserName) {
                for (int i = 0; i < clientList.size(); i++) {
                    if (clientList.get(i).getUserName().equals(userName) || !userName.matches("^[a-zA-Z0-9]*$")
                            || userName.length() == 0) {
                        privateBroadcast("Sorry that user name is either taken or invalid, please enter another username.", client);
                        userName = in.readLine().trim().substring(5);
                        break;
                    }

                    if(i == clientList.size()-1){
                        validUserName = true;
                    }
                }
            }
            client.setUserName(userName);
            //notify all that client has joined
            broadcast(String.format("WELCOME %s", client.getUserName()));

            privateBroadcast("Current Members in server:", client);
            for(int i = 0; i < clientList.size(); i++){
                privateBroadcast(clientList.get(i).getUserName(), client);
            }


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

                    ArrayList<String> pchatUsername = new ArrayList<String>();
                    pchatUsername.add(chat.substring(0, endOfUsernameIndex).trim());
                    chat = chat.substring(endOfUsernameIndex).trim();

                    while(chat.startsWith("@")){
                        endOfUsernameIndex = chat.indexOf(" ");
                        pchatUsername.add(chat.substring(1, endOfUsernameIndex));
                        chat = chat.substring(endOfUsernameIndex).trim();
                    }


                    for (ClientConnectionData c: clientList) {
                        for(int i = 0; i < pchatUsername.size(); i++) {
                            if (pchatUsername.get(i).equals(c.getUserName())) {
                                if (chat.length() > 0) {
                                    String receiverMSG = String.format("PCHAT %s %s", client.getUserName(), chat);
                                    String senderMSG = String.format("PCHAT %s %s", c.getUserName(), chat);
                                    privateBroadcast(receiverMSG, c);
                                    privateBroadcast(senderMSG, client);

                                }
                            }
                        }
                    }
                }

                else if (incoming.startsWith("Die Roll: ")){
                    String msg = client.getUserName() + "'s " + incoming.trim();
                    broadcast(msg);
                }

                else if (incoming.startsWith("Coin Flip: ")){
                    String msg = client.getUserName() + "'s " + incoming.trim();
                    broadcast(msg);
                }

                else if (incoming.startsWith("WhoIsHere")){
                    privateBroadcast("Current Members in server:", client);
                    for(int i = 0; i < clientList.size(); i++){
                        privateBroadcast(clientList.get(i).getUserName(), client);
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