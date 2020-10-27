import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
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
    public void broadcast(Serialization msg) {
        try {
            System.out.println("Broadcasting -- " + msg);
            synchronized (clientList) {
                for (ClientConnectionData c : clientList){

                    if(msg.getMsgHeader() == Serialization.MSG_HEADER_CHAT) {
                        if (c.getUserName().length() != 0 && client.getUserName() != c.getUserName()) {
                            c.getOut().writeObject(msg);
                        }
                    }
                    else{
                        if(c.getUserName().length() != 0) {
                            c.getOut().writeObject(msg);
                        }
                    }


                }
            }
        } catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }

    }

    public void privateBroadcast(Serialization msg, ClientConnectionData c) {
        try {
            System.out.println("Private Broadcasting to " + c.getUserName() + " -- " + msg);
            c.getOut().writeObject(msg);
        }
        catch (Exception ex) {
            System.out.println("broadcast caught exception: " + ex);
            ex.printStackTrace();
        }
    }

    public void whoIsHere(ClientConnectionData c) {
        Serialization temp = new Serialization(Serialization.MSG_HEADER_OTHER, "Current Members in server:");
        privateBroadcast(temp, client);
        String list = "";
        for(int i = 0; i < clientList.size(); i++){
            list = list + clientList.get(i).getUserName() + " ";
        }
        temp = new Serialization(Serialization.MSG_HEADER_WHOISHERE, list);
        System.out.println(list);
        privateBroadcast(temp, client);
    }

    @Override
    public void run() {
        exit: try {
            ObjectInputStream in = new ObjectInputStream(client.getSocket().getInputStream());
            //get userName, first message from user
            Serialization incoming;

            String userName = ((Serialization)in.readObject()).getMsg().trim();
            Boolean validUserName = false;

            // checks that username is unique

            while(!validUserName) {
                for (int i = 0; i < clientList.size(); i++) {
                    if (clientList.get(i).getUserName().equals(userName) || !userName.matches("^[a-zA-Z0-9]*$")
                            || userName.length() == 0) {
                        Serialization temp = new Serialization(Serialization.MSG_HEADER_INVALIDNAME, "Sorry that user name is either taken or invalid, please enter another username.");
                        privateBroadcast(temp, client);
                        userName = ((Serialization)in.readObject()).getMsg().trim();
                        System.out.println(userName);
                        break;
                    }

                    if(i == clientList.size()-1){
                        validUserName = true;
                    }
                }
            }
            client.setUserName(userName);
            //notify all that client has joined
            Serialization temp = new Serialization(Serialization.MSG_HEADER_WELCOME, client.getUserName());
            broadcast(temp);

            whoIsHere(client);
            //incoming = (Serialization) in.readObject();

            //System.out.println(incoming);

            while( (incoming = (Serialization) in.readObject()) != null) {
                if(incoming.getMsgHeader() == Serialization.MSG_HEADER_CHAT) {
                    String chat = incoming.getMsg().trim();
                    if (chat.length() > 0) {
                        String msg = String.format("%s %s", client.getUserName(), chat);
                        broadcast(new Serialization(Serialization.MSG_HEADER_CHAT, msg));
                    }
                }

                else if(incoming.getMsgHeader() == Serialization.MSG_HEADER_PRIVATECHAT) {
                    //String chat = incoming.getMsg().substring(7).trim();
                    String chat = incoming.getMsg().trim();
                    int endOfUsernameIndex = chat.indexOf(" ");

                    ArrayList<String> pchatUsername = new ArrayList<String>();
                    pchatUsername.add(chat.substring(1, endOfUsernameIndex).trim());
                    chat = chat.substring(endOfUsernameIndex).trim();

                    while(chat.startsWith("@")){
                        endOfUsernameIndex = chat.indexOf(" ");
                        pchatUsername.add(chat.substring(1, endOfUsernameIndex));
                        chat = chat.substring(endOfUsernameIndex).trim();
                    }

                    for (ClientConnectionData c: clientList) {
                        for(int i = 0; i < pchatUsername.size(); i++) {
                            if (pchatUsername.get(i).equals(c.getUserName()) && !pchatUsername.get(i).equals(client.getUserName())) {
                                if (chat.length() > 0) {
                                        System.out.println(chat);
                                        temp = new Serialization(Serialization.MSG_HEADER_PRIVATECHAT, String.format("%s %s", client.getUserName(), chat));
                                        privateBroadcast(temp, c);
                                        //privateBroadcast(temp, client);
                                }
                            }
                        }
                    }
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_DIEROLL){
                    int roll =  (int) ((Math.random()*6)+1);
                    String msg = client.getUserName() + "'s" + " DIE ROLL: " + roll;
                    broadcast(new Serialization(Serialization.MSG_HEADER_DIEROLL, msg));
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_COINFLIP){
                    int rand = (int) ((Math.random()*2)+1);
                    String msg = "";
                    if(rand == 1) {
                        msg = client.getUserName() + "'s " + "COIN FLIP: HEADS";
                    } else if (rand == 2) {
                        msg = client.getUserName() + "'s " + "COIN FLIP: TAILS";
                    }
                    broadcast(new Serialization(Serialization.MSG_HEADER_COINFLIP, msg));
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_WHOISHERE){
                    whoIsHere(client);
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_QUIT){
                    //broadcast(new Serialization(Serialization.MSG_HEADER_QUIT, client.getUserName()));
                    break exit;
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
            broadcast(new Serialization(Serialization.MSG_HEADER_QUIT, client.getUserName()));
            try {
                client.getSocket().close();
            } catch (IOException ex) {}

        }
    }

}