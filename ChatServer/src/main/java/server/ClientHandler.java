package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class ClientHandler {
    
    final ClientHandler instance = this;

    private MainServer server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    
    private String userNick;
    
    private boolean alive = false;
    
    public String getUserNick() {
        return userNick;
    }
    
    public void setUserNick(String userNick) {
        this.userNick = userNick;
    }
    
    public ClientHandler(MainServer server, Socket socket) {
        
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            
            new Timer(true).schedule(new TimerTask() {
                @Override
                public void run() {
                    if(alive == false) {
                        cancel();
                        closeConnection();
                    }
                    alive = false;
                }
            }, 120*1000, 120 * 1000);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while(true) {
                            String str = in.readUTF();
                            alive = true;
                            if(str.startsWith("/auth")) {
                                String[] parts = str.split(" ");
                                String login = parts[1];
                                String pass = parts[2];
                                
                                String newNick = AuthService.getNickByLoginAndPass(login, pass, true); //Нужна проверка в БД
                                
                                if(newNick != null) {
                                    if(!server.isNickBusy(newNick)) { //проверяем, занят ли ник
                                        sendMsg("/authok");
                                        setUserNick(newNick);
                                        server.addClient(ClientHandler.this);
                                        break;
                                    } else {
                                        sendMsg("Данный логин уже занят!");
                                    }
                                } else {
                                    sendMsg("Неверный логин или пароль!");
                                }
                            }
                        }
                        
                        while (true) {
                            String str = in.readUTF();
                            
                            if(str.startsWith("/")) {
                                if (str.equals("/end")) {
                                    out.writeUTF("/serverClosed");
                                    break;
                                }
                                if(str.startsWith("/w ")) {
                                    String[] parts = str.split(" ",3);
                                    server.sendMsg(ClientHandler.this, parts[1], parts[2]);
                                }
                                if(str.startsWith("/keepAliveMsg")) {
                                    alive = true;
                                }
                            } else {
                                server.broadCastMsg(ClientHandler.this, str);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        closeConnection();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void closeConnection() {
        try {
            in.close();
            out.close();
            socket.close();
            server.deleteClient(instance);
            System.out.println("Клиент отключился!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
