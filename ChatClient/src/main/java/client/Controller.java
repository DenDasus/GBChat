package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class Controller implements Initializable {
    
    @FXML
    VBox messagesVBox;
    @FXML
    TextField textField;
    @FXML
    Pane pane;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    VBox authPanel;
    @FXML
    HBox messageSendPanel;
    @FXML
    ListView<String> clientsList;
    @FXML
    ScrollPane messagesBoxScrollPane;
    @FXML
    VBox registrationPanel;
    @FXML
    VBox updateUserDataPanel;
    @FXML
    TextField regLoginField;
    @FXML
    PasswordField regPasswordField;
    @FXML
    PasswordField regPasswordRepeatField;
    @FXML
    TextField regNickField;
    @FXML
    VBox rightPanel;
    @FXML
    PasswordField newPasswordField;
    @FXML
    PasswordField newPasswordRepeatField;
    @FXML
    TextField newNickField;
    @FXML
    PasswordField currentPasswordField;

    Socket socket;

    DataInputStream in;
    DataOutputStream out;
    
    final String separator = "=!=";
    final String IP_ADRESS = "localhost";
    final int PORT = 8189;
    
    private boolean authorized = false;
    private String myLogin;
    
    private Timer keepAliveTimer;
    
    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
        
        if(!authorized) {
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            messageSendPanel.setVisible(false);
            messageSendPanel.setManaged(false);
            rightPanel.setVisible(false);
            rightPanel.setManaged(false);
        } else {
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            messageSendPanel.setVisible(true);
            messageSendPanel.setManaged(true);
            rightPanel.setVisible(true);
            rightPanel.setManaged(true);
            rightPanel.setPrefWidth(150);
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        try {
            setAuthorized(false);
            socket = new Socket(IP_ADRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            
            keepAliveTimer = new Timer(true);
            keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
                /**
                 * The action to be performed by this timer task.
                 */
                @Override
                public void run() {
                    try {
                        out.writeUTF("/keepAliveMsg");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }, 10*1000, 100 * 1000);

            Thread socketListener = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            String str = in.readUTF();
                            if (str.equals("/serverClosed")) break;
                            if (str.equals("/authok")) {
                                setAuthorized(true);
                                break;
                            }
                            if(str.equals("/regOk")) {
                                closeRegistrationPanel();
                                setAuthorized(false);
                                String messageStr = "Регистрация прошла успешно. Теперь Вы можете войти.";
                                printMessage("System", "", messageStr, false, true);
                                continue;
                            }
                            
                            printMessage("System", "", str, false, true);
                        }
                        
                        while (true) {
                            String str = in.readUTF();
                            
                            if(str.startsWith("/")){
                                MessageCell msg = null;
                                
                                if(str.startsWith("/bMsg")) {
                                    String[] parts = str.split(separator, 4);
                                    boolean income = !parts[2].equals(myLogin);
                                    msg = new MessageCell(parts[2], parts[1], parts[3], false, income);
                                }
    
                                if(str.startsWith("/pMsg")) {
                                    String[] parts = str.split(separator, 4);
                                    boolean income = !parts[2].equals(myLogin);
                                    msg = new MessageCell(parts[2], parts[1], parts[3], true, income);
                                }
                                
                                if(str.startsWith("/sysMsg")) {
                                    String[] parts = str.split(separator, 4);
                                    if(parts[2].equals("clientsList")) {
                                        String[] list = parts[3].split(" ");
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                clientsList.getItems().clear();
                                                for (int i = 0; i < list.length; i++) {
                                                    clientsList.getItems().add(list[i]);
                                                }
                                            }
                                        });
                                    }
                                }
                                
                                if(str.startsWith("/updateNickOk")) {
                                    printMessage("System", "", "Ник успешно обновлен", false, true);
                                }
                                
                                if(str.startsWith("/updatePassOk")) {
                                    printMessage("System", "", "Пароль успешно обновлен", false, true);
                                }
                                
                                if(msg != null) {
                                    final MessageCell msgPtr = msg;
                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            messagesVBox.getChildren().add(msgPtr);
                                        }
                                    });
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            socketListener.setDaemon(true);
            socketListener.start();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void tryToLogin() {
        String login = loginField.getText();
        String pass = passwordField.getText();
        if(login.matches("^[\\w]{3,15}$") && !pass.equals("")) {
            try {
                out.writeUTF("/auth " + login + " " + pass);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String messageStr = "Имя пользователя должно состоять из букв латинского алфавита или цифр. Длина имени от 3 до 15 символов.";
            printMessage("System", "", messageStr, false, true);
        }
        loginField.clear();
        passwordField.clear();
        
        myLogin = login;
    }
    
    public void openRegistrationPanel() {
        authPanel.setVisible(false);
        authPanel.setManaged(false);
        messageSendPanel.setVisible(false);
        messageSendPanel.setManaged(false);
        rightPanel.setVisible(false);
        rightPanel.setManaged(false);
        registrationPanel.setVisible(true);
        registrationPanel.setManaged(true);
    }
    
    public void closeRegistrationPanel() {
        authPanel.setVisible(true);
        authPanel.setManaged(true);
        messageSendPanel.setVisible(false);
        messageSendPanel.setManaged(false);
        rightPanel.setVisible(false);
        rightPanel.setManaged(false);
        registrationPanel.setVisible(false);
        registrationPanel.setManaged(false);
    }
    
    public void tryToRegister() {
        String login = regLoginField.getText();
        String pass = regPasswordField.getText();
        String repeat = regPasswordRepeatField.getText();
        String nick = regNickField.getText();
        if(!login.matches("^[\\w]{3,15}$"))
        {
            String messageStr = "Имя пользователя должно состоять из букв латинского алфавита или цифр. Длина имени от 3 до 15 символов.";
            printMessage("System", "", messageStr, false, true);
            return;
        }
        
        if(pass.equals("")) {
            String messageStr = "Заполните поле пароль";
            printMessage("System", "", messageStr, false, true);
            return;
        }
        
        if(!pass.equals(repeat)) {
            String messageStr = "Введенные пароли не совпадают";
            printMessage("System", "", messageStr, false, true);
            return;
        }
    
        if(nick.equals("")) {
            String messageStr = "Ник не может быть пустым";
            printMessage("System", "", messageStr, false, true);
            return;
        }
    
        try {
            out.writeUTF("/reg " + login + " " + pass + " " + nick);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void closeUpdateUserDataPanel() {
        updateUserDataPanel.setManaged(false);
        updateUserDataPanel.setVisible(false);
        newNickField.clear();
        newPasswordField.clear();
        newPasswordRepeatField.clear();
        currentPasswordField.clear();
    }
    
    public void openUpdateUserDataPanel() {
        updateUserDataPanel.setManaged(true);
        updateUserDataPanel.setVisible(true);
    }
    
    public void updateUserData() {
        String pass = newPasswordField.getText();
        String repeat = newPasswordRepeatField.getText();
        String nick = newNickField.getText();
        String currPass = currentPasswordField.getText();
        if(!pass.equals(repeat)) {
            String messageStr = "Введенные пароли не совпадают";
            printMessage("System", "", messageStr, false, true);
            return;
        }
        if(nick.equals("") && pass.equals("")) {
            String messageStr = "Изменения отсутвуют";
            printMessage("System", "", messageStr, false, true);
            closeUpdateUserDataPanel();
            return;
        }
        
        if(!nick.equals("")) {
            try {
                out.writeUTF("/updateNick " + myLogin + " " + currPass + " " + nick);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
        if(!pass.equals("")) {
            try {
                out.writeUTF("/updatePass " + myLogin + " " + currPass + " " + pass);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        closeUpdateUserDataPanel();
    }
    
    public void printMessage(String nameStr, String timeStr, String messageStr, boolean privateMsg, boolean income) {
        MessageCell msg = new MessageCell(nameStr, timeStr, (messageStr + "\n"), privateMsg, income);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                messagesVBox.getChildren().add(msg);
            }
        });
    }
}
