package server;

import java.sql.*;

public class AuthService {
    
    private static Connection connection;
    private static Statement statement;
    
    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:users.db");
            statement = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void addUser(String login, String pass, String nick) {
        try {
            String query = "INSERT INTO main (login, password, nickname) VALUES (?, ?, ?);";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setInt(2, pass.hashCode());
            ps.setString(3, nick);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static String getNickByLoginAndPass(String login, String pass, boolean useDB) {
        if(!useDB) {
            if(pass.equals("111")) {
                return null;
            }
            return login;
        }
        
        try {
            ResultSet rs = statement.executeQuery("SELECT nickname, password FROM main WHERE login = '" + login + "'");
            int myHash = pass.hashCode();
            if (rs.next()) {
                String nick = rs.getString(1);
                int dbHash = rs.getInt(2);
                if (myHash == dbHash) {
                    return nick;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static boolean isUserExist(String login) {
        boolean exist = false;
        try {
            ResultSet rs = statement.executeQuery("SELECT login FROM main WHERE login = '" + login + "'");
            if(rs.next()) {
                exist = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exist;
    }
    
    public static boolean updateNick(String login, String currPass, String newNick) {
        if(getNickByLoginAndPass(login, currPass, true) != null) {
            String updateNick = "UPDATE main SET nickname = ? WHERE login = ?";
            try {
                PreparedStatement ps = connection.prepareStatement(updateNick);
                ps.setString(1, newNick);
                ps.setString(2, login);
                int cnt = ps.executeUpdate();
                System.out.println(cnt);
                if(cnt > 0) {
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public static boolean updatePass(String login, String currPass, String newPass) {
        if(getNickByLoginAndPass(login, currPass, true) != null) {
            String updatePass = "UPDATE main SET password = ? WHERE login = ?";
            try {
                PreparedStatement ps = connection.prepareStatement(updatePass);
                ps.setInt(1, newPass.hashCode());
                ps.setString(2, login);
                int cnt = ps.executeUpdate();
                System.out.println(cnt);
                if(cnt > 0) {
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    
    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
