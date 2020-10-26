import java.io.BufferedReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionData {
    private Socket socket;
    private BufferedReader input;
    private ObjectOutputStream out;
    private String name;
    private String userName;

    public ClientConnectionData(Socket socket, BufferedReader input, ObjectOutputStream out, String name) {
        this.socket = socket;
        this.input = input;
        this.out = out;
        this.name = name;
        this.userName = "";
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public BufferedReader getInput() {
        return input;
    }

    public void setInput(BufferedReader input) {
        this.input = input;
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public void setOut(ObjectOutputStream out) {
        this.out = out;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }


}
