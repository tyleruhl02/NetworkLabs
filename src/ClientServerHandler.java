import java.io.BufferedReader;
import java.io.ObjectInputStream;

public class ClientServerHandler implements Runnable{
    ObjectInputStream socketIn;

    public ClientServerHandler(ObjectInputStream socketIn){
        this.socketIn = socketIn;
    }

    @Override
    public void run() {
        try {
            Serialization incoming;

            while((incoming = (Serialization) socketIn.readObject()) != null) {
                //handle different headers
                //WELCOME
                //CHAT
                //EXIT
                String msg = incoming.getMsg();
                if(incoming.getMsgHeader() == Serialization.MSG_HEADER_CHAT) {
                    msg = msg.substring(0, msg.indexOf(" ")).trim() + ": " + msg.substring(msg.indexOf(" ")).trim();
                    System.out.println(msg);
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_PRIVATECHAT) {
                    String firstUsername = msg.substring(0, msg.indexOf(" "));
                    msg = msg.substring(msg.indexOf(" ")).trim();
                    String secondUsername = msg.substring(0, msg.indexOf(" "));
                    msg = msg.substring(msg.indexOf(" ")).trim();
                    System.out.println(firstUsername + " --> " + secondUsername + ": " + msg);
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_DIEROLL) {
                    System.out.println(msg);
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_COINFLIP) {
                    System.out.println(msg);
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_WHOISHERE) {
                    String[] chat = incoming.getMsg().trim().split(" ");
                    for (int i = 0; i < chat.length; i++){
                        System.out.println(chat[i]);
                    }
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_OTHER) {
                    System.out.println(msg);
                }

                else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_QUIT){
                    System.out.println(incoming.getMsg() + " has left the server.");
                }

                else {
                    System.out.println("Invalid Message Header.");
                }
                //System.out.println(incoming.getMsg());
            }
        } catch (Exception ex) {
            System.out.println("Exception caught in listener - " + ex);
        } finally{
            System.out.println("ClientServerHandler exiting");
        }
    }
}