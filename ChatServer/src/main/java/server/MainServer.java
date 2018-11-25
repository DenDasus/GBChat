package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class MainServer {

    final Logger logger = LogManager.getLogger(MainServer.class);

    Vector<ClientHandler> clients;
    private SimpleDateFormat formatTime = new SimpleDateFormat("hh:mm");
    final String separator = "=!=";

    public MainServer() {
    
        AuthService.connect();

        ServerSocket server = null;
        Socket socket = null;
        clients = new Vector<>();

        try {
            server = new ServerSocket(8189);
            logger.debug("Сервер запущен!");

            while (true) {
                socket = server.accept();
                logger.debug("Клиент подключился!");
                new ClientHandler(this, socket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            AuthService.disconnect();
        }

    }

    public void broadCastMsg(ClientHandler from, String msg) {
        for (ClientHandler o : clients) {
            String timeStr = formatTime.format(new Date());
            o.sendMsg("/bMsg" + separator + timeStr + separator + from.getUserNick() + separator + msg);
        }
    }
    
    public void sendMsg(ClientHandler from, String to, String msg) {
        for (ClientHandler o : clients) {
            if(o.getUserNick().equals(to)) {
                String timeStr = formatTime.format(new Date());
                o.sendMsg("/pMsg" + separator + timeStr + separator + from.getUserNick() + separator + msg);
                from.sendMsg("/pMsg" + separator + timeStr + separator + from.getUserNick() + separator + msg);
                return;
            }
        }
    }
    
    public void sendSystemMsg(ClientHandler to, String type, String msg) {
        String timeStr = formatTime.format(new Date());
        to.sendMsg("/sysMsg" + separator + timeStr + separator + type + separator + msg);
    }

    public void deleteClient(ClientHandler client) {
        clients.removeElement(client);
        broadcastClientList();
    }
    
    public void addClient(ClientHandler client) {
        clients.add(client);
        broadcastClientList();
    }
    
    public boolean isNickBusy(String nick) {
        for (ClientHandler o : clients) {
            if(o.getUserNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }
    
    public void broadcastClientList() {
        StringBuilder clientsListString = new StringBuilder();
    
        for (ClientHandler o : clients) {
            clientsListString.append(o.getUserNick() + " ");
        }
        
        for (ClientHandler o : clients) {
            sendSystemMsg(o, "clientsList", clientsListString.toString());
        }
    }
}
