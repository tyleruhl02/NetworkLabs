import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
/**
 * For Java 8, javafx is installed with the JRE. You can run this program normally.
 * For Java 9+, you must install JavaFX separately: https://openjfx.io/openjfx-docs/
 * If you set up an environment variable called PATH_TO_FX where JavaFX is installed
 * you can compile this program with:
 *  Mac/Linux:
 *      > javac --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows CMD:
 *      > javac --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 *  Windows Powershell:
 *      > javac --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui/ChatGuiClient.java
 * 
 * Then, run with:
 * 
 *  Mac/Linux:
 *      > java --module-path $PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient 
 *  Windows CMD:
 *      > java --module-path %PATH_TO_FX% --add-modules javafx.controls day10_chatgui.ChatGuiClient
 *  Windows Powershell:
 *      > java --module-path $env:PATH_TO_FX --add-modules javafx.controls day10_chatgui.ChatGuiClient
 * 
 * There are ways to add JavaFX to your to your IDE so the compile and run process is streamlined.
 * That process is a little messy for VSCode; it is easiest to do it via the command line there.
 * However, you should open  Explorer -> Java Projects and add to Referenced Libraries the javafx .jar files 
 * to have the syntax coloring and autocomplete work for JavaFX 
 */

class ServerInfo {
    public final String serverAddress;
    public final int serverPort;

    public ServerInfo(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }
}

public class ChatGuiClient extends Application {
    private Socket socket;
    private static ObjectInputStream socketIn;
    private static ObjectOutputStream out;
    
    private Stage stage;
    private TextArea messageArea;
    private TextField textInput;
    private Button sendButton;
    private Button coinflipButton;
    private Button rolldieButton;
    private TextArea userList;

    private ServerInfo serverInfo;
    //volatile keyword makes individual reads/writes of the variable atomic
    // Since username is accessed from multiple threads, atomicity is important 
    private volatile String username = "";
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //If ip and port provided as command line arguments, use them
        List<String> args = getParameters().getUnnamed();
        if (args.size() == 2){
            this.serverInfo = new ServerInfo(args.get(0), Integer.parseInt(args.get(1)));
        }
        else {
            //otherwise, use a Dialog.
            Optional<ServerInfo> info = getServerIpAndPort();
            if (info.isPresent()) {
                this.serverInfo = info.get();
            } 
            else{
                Platform.exit();
                return;
            }
        }

        this.stage = primaryStage;
        BorderPane borderPane = new BorderPane();

        messageArea = new TextArea();
        messageArea.setWrapText(true);
        messageArea.setEditable(false);
        borderPane.setCenter(messageArea);

        userList = new TextArea();
        userList.setWrapText(true);
        userList.setEditable(false);
        borderPane.setTop(userList);
        //userList.appendText("TEST");

        //At first, can't send messages - wait for WELCOME!
        textInput = new TextField();
        textInput.setEditable(false);
        textInput.setOnAction(e -> sendMessage());
        sendButton = new Button("Send");
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendMessage());

        coinflipButton = new Button("Flip Coin");
        coinflipButton.setDisable(true);
        coinflipButton.setOnAction(e -> flipcoin());

        rolldieButton = new Button("Roll Die");
        rolldieButton.setDisable(true);
        rolldieButton.setOnAction(e -> rolldie());

        HBox hbox = new HBox();
        hbox.getChildren().addAll(new Label("Message: "), textInput, sendButton, coinflipButton, rolldieButton);
        HBox.setHgrow(textInput, Priority.ALWAYS);
        borderPane.setBottom(hbox);

        Scene scene = new Scene(borderPane, 400, 500);
        stage.setTitle("Chat Client");
        stage.setScene(scene);
        stage.show();

        ServerListener socketListener = new ServerListener();
        
        //Handle GUI closed event
        stage.setOnCloseRequest(e -> {
            try {
                out.writeObject(new Serialization(Serialization.MSG_HEADER_OTHER, "QUIT"));
                socketListener.appRunning = false;
                socket.close(); 
            } catch (IOException ex) {}
        });

        new Thread(socketListener).start();
    }

    private void flipcoin() {
        try{
            Serialization m = new Serialization(Serialization.MSG_HEADER_COINFLIP, "/flipcoin");
            out.writeObject(m);
        } catch(IOException ex) {
            System.out.println("Error");
        }
    }

    private void rolldie() {
        try{
            Serialization m = new Serialization(Serialization.MSG_HEADER_DIEROLL, "/rolldie");
            out.writeObject(m);
        } catch(IOException ex) {
            System.out.println("Error");
        }
    }

    private void sendMessage() {
        try {
            String message = textInput.getText().trim();
            if (message.length() == 0)
                return;
            textInput.clear();
            Serialization m;
            if (message.startsWith("@")) {
                m = new Serialization(Serialization.MSG_HEADER_PRIVATECHAT, message);

                int endOfUsernameIndex = message.indexOf(" ");

                ArrayList<String> pchatUsername = new ArrayList<String>();
                pchatUsername.add(message.substring(1, endOfUsernameIndex).trim());
                String tempMes = message.substring(endOfUsernameIndex).trim();

                while(tempMes.startsWith("@")){
                    endOfUsernameIndex = tempMes.indexOf(" ");
                    pchatUsername.add(tempMes.substring(1, endOfUsernameIndex));
                    tempMes = tempMes.substring(endOfUsernameIndex).trim();
                }

                String msg = tempMes;

                for(int i = 0; i < pchatUsername.size(); i++){
                    String name = pchatUsername.get(i);
                    Platform.runLater(() -> {
                        messageArea.appendText("To  " + name + " (privately): " + msg + "\n");
                    });
                }

            } else if(message.startsWith("/rolldie")) {
                m = new Serialization(Serialization.MSG_HEADER_DIEROLL, message);
            } else if(message.startsWith("/flipcoin"))  {
                m = new Serialization(Serialization.MSG_HEADER_COINFLIP, message);
            } else if(message.startsWith("/whoishere")) {
                m = new Serialization(Serialization.MSG_HEADER_WHOISHERE, message);
            } else if (message.startsWith("/quit")){
                m = new Serialization(Serialization.MSG_HEADER_QUIT, "");
            } else {
                m = new Serialization(Serialization.MSG_HEADER_CHAT, message);
                Platform.runLater(() -> {
                    messageArea.appendText(username + ": " + message + "\n");
                });
            }
            out.writeObject(m);
        } catch(IOException ex){
            System.out.println("Error");
        }

    }

    private Optional<ServerInfo> getServerIpAndPort() {
        // In a more polished product, we probably would have the ip /port hardcoded
        // But this a great way to demonstrate making a custom dialog
        // Based on Custom Login Dialog from https://code.makery.ch/blog/javafx-dialogs-official/

        // Create a custom dialog for server ip / port
        Dialog<ServerInfo> getServerDialog = new Dialog<>();
        getServerDialog.setTitle("Enter Server Info");
        getServerDialog.setHeaderText("Enter your server's IP address and port: ");

        // Set the button types.
        ButtonType connectButtonType = new ButtonType("Connect", ButtonData.OK_DONE);
        getServerDialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Create the ip and port labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField ipAddress = new TextField();
        ipAddress.setPromptText("e.g. localhost, 127.0.0.1");
        grid.add(new Label("IP Address:"), 0, 0);
        grid.add(ipAddress, 1, 0);

        TextField port = new TextField();
        port.setPromptText("e.g. 54321");
        grid.add(new Label("Port number:"), 0, 1);
        grid.add(port, 1, 1);


        // Enable/Disable connect button depending on whether a address/port was entered.
        Node connectButton = getServerDialog.getDialogPane().lookupButton(connectButtonType);
        connectButton.setDisable(true);

        // Do some validation (using the Java 8 lambda syntax).
        ipAddress.textProperty().addListener((observable, oldValue, newValue) -> {
            connectButton.setDisable(newValue.trim().isEmpty());
        });

        port.textProperty().addListener((observable, oldValue, newValue) -> {
            // Only allow numeric values
            if (! newValue.matches("\\d*"))
                port.setText(newValue.replaceAll("[^\\d]", ""));

            connectButton.setDisable(newValue.trim().isEmpty());
        });

        getServerDialog.getDialogPane().setContent(grid);
        
        // Request focus on the username field by default.
        Platform.runLater(() -> ipAddress.requestFocus());


        // Convert the result to a ServerInfo object when the login button is clicked.
        getServerDialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new ServerInfo(ipAddress.getText(), Integer.parseInt(port.getText()));
            }
            return null;
        });

        return getServerDialog.showAndWait();
    }

    private String getName(int prompt){
        TextInputDialog nameDialog = new TextInputDialog();
        username = "";
        nameDialog.setTitle("Enter Chat Name");
        if(prompt == 0){
            nameDialog.setHeaderText("Please enter your username.");
        }
        else if (prompt == 1) {
            nameDialog.setHeaderText("That username was taken. Enter a different username.");
        }
        nameDialog.setContentText("Name: ");
        
        while(username.equals("")) {
            Optional<String> name = nameDialog.showAndWait();
            if (!name.isPresent() || name.get().trim().equals(""))
                nameDialog.setHeaderText("You must enter a nonempty name: ");
            else if (name.get().trim().contains(" "))
                nameDialog.setHeaderText("Your name must have no spaces: ");
            else if (!name.get().trim().matches("^[a-zA-Z0-9]*$")){
                nameDialog.setHeaderText("Your name must have only alphanumeric characters: ");
            }
            else
            username = name.get().trim();            
        }
        return username;
    }

    class ServerListener implements Runnable {

        volatile boolean appRunning = false;

        public void run() {
            exit: try {
                // Set up the socket for the Gui
                socket = new Socket(serverInfo.serverAddress, serverInfo.serverPort);
                socketIn = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(socket.getOutputStream());
                
                appRunning = true;
                //Ask the gui to show the username dialog and update username
                //Send to the server
                Platform.runLater(() -> {
                    try {
                        out.writeObject(new Serialization(Serialization.MSG_HEADER_OTHER, getName(0)));
                    } catch(IOException Exception){
                        System.out.println("Error");
                    }

                });

                Serialization incoming;

                //handle all kinds of incoming messages
                while (appRunning && (incoming = (Serialization) socketIn.readObject()) != null) {
                    if (incoming.getMsgHeader() == Serialization.MSG_HEADER_WELCOME) {
                        String user = incoming.getMsg();
                        //got welcomed? Now you can send messages!
                        if (user.equals(username)) {
                            Platform.runLater(() -> {
                                stage.setTitle("Chatter - " + username);
                                textInput.setEditable(true);
                                sendButton.setDisable(false);
                                coinflipButton.setDisable(false);
                                rolldieButton.setDisable(false);
                                messageArea.appendText(username + " has joined the server." + "\n");
                            });
                        }
                        else {
                            Platform.runLater(() -> {
                                messageArea.appendText(user + " has joined the server.\n");
                            });
                        }

                    } else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_CHAT) {
                        String msg = incoming.getMsg();
                        String finalMsg = msg.substring(0, msg.indexOf(" ")).trim() + ": " + msg.substring(msg.indexOf(" ")).trim();

                        Platform.runLater(() -> {
                            messageArea.appendText(finalMsg + "\n");
                        });
                    }
                    else if (incoming.getMsgHeader()==Serialization.MSG_HEADER_QUIT) {
                        String user = incoming.getMsg();
                        Platform.runLater(() -> {
                            messageArea.appendText(user + " has left the server.\n");
                        });
                    }
                    else if(incoming.getMsgHeader()==Serialization.MSG_HEADER_WHOISHERE){
                        String[] chat = incoming.getMsg().trim().split(" ");
                        for (int i = 0; i < chat.length; i++){
                            String msg = chat[i];
                            Platform.runLater(() -> {
                                messageArea.appendText(msg + "\n");
                            });
                        }
                    }
                    else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_USERLIST) {
                        String msg = incoming.getMsg();
                        Platform.runLater(() -> {
                            userList.clear();
                            userList.appendText("USERS IN SERVER:\n");
                            userList.appendText(msg);
                        });
                    }
                    else if(incoming.getMsgHeader()==Serialization.MSG_HEADER_PRIVATECHAT){
                        String msg = incoming.getMsg();
                        String firstUsername = msg.substring(0, msg.indexOf(" "));
                        String finalMsg = msg.substring(msg.indexOf(" ")).trim();
                        //String secondUsername = msg.substring(0, msg.indexOf(" "));
                        //msg = msg.substring(msg.indexOf(" ")).trim();
                        Platform.runLater(() -> {
                            messageArea.appendText(firstUsername + " (privately): " + finalMsg + "\n");
                        });
                    }
                    else if(incoming.getMsgHeader()==Serialization.MSG_HEADER_COINFLIP){
                        String msg = incoming.getMsg();
                        Platform.runLater(() -> {
                            messageArea.appendText(msg + "\n");
                        });
                    }
                    else if(incoming.getMsgHeader()==Serialization.MSG_HEADER_DIEROLL){
                        String msg = incoming.getMsg();
                        Platform.runLater(() -> {
                            messageArea.appendText(msg + "\n");
                        });
                    }

                    else if (incoming.getMsgHeader()==Serialization.MSG_HEADER_QUIT) {
                        String user = incoming.getMsg();
                        Platform.runLater(() -> {
                            messageArea.appendText(user + " has left the server." + "\n");
                        });
                        break exit;
                    }

                    else if (incoming.getMsgHeader() == Serialization.MSG_HEADER_INVALIDNAME) {
                        Platform.runLater(() -> {
                            try {
                                out.writeObject(new Serialization(Serialization.MSG_HEADER_OTHER, getName(1)));
                            } catch(IOException Exception){
                                System.out.println("Error");
                            }

                        });
                    }

                    else if(incoming.getMsgHeader()==Serialization.MSG_HEADER_OTHER){
                        String msg = incoming.getMsg();
                        Platform.runLater(() -> {
                            messageArea.appendText(msg + "\n");
                        });
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (Exception e) {
                if (appRunning)
                    e.printStackTrace();
            } 
            finally {
                Platform.runLater(() -> {
                    stage.close();
                });
                try {
                    if (socket != null)
                        socket.close();
                }
                catch (IOException e){
                }
            }
        }
    }
}