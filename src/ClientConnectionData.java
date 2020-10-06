import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionData {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter out;
    private String name;
    private String userName;

    public ClientConnectionData(Socket socket, BufferedReader input, PrintWriter out, String name) {
        this.socket = socket;
        this.input = input;
        this.out = out;
        this.name = name;
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

    public PrintWriter getOut() {
        return out;
    }

    public void setOut(PrintWriter out) {
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
