/*
 * This file is part of BlitzLogin, licensed under the MIT License.
 *
 *  Copyright (c) 2021 XXMA16
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package tk.blitzfarm.blitzlogin.velocity.util;

import java.sql.*;

public class DatabaseUtil {
    private final Connection connection;

    public DatabaseUtil(String address, String database, String username, String password) throws ClassNotFoundException, SQLException {
        synchronized (this) {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + address + "/" + database + "?autoReconnect=true", username, password);
            PreparedStatement ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS user_profiles (username VARCHAR(16),uniqueId CHAR(36),premium BOOLEAN,hashedPassword CHAR(64),sessionStart TIMESTAMP NULL,lastAddress VARCHAR(40),PRIMARY KEY (username))");
            ps.executeUpdate();
        }
    }

    public void createPlayer(String username, String uuid, boolean premium) {
        try {
            if (!exists(username)) {
                PreparedStatement ps = connection.prepareStatement("INSERT IGNORE INTO user_profiles (username,uniqueId,premium) VALUES (?,?,?)");
                ps.setString(1, username);
                ps.setString(2, uuid);
                ps.setBoolean(3, premium);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void unregisterPlayer(String username) {
        try {
            PreparedStatement ps = connection
                    .prepareStatement("UPDATE user_profiles SET hashedPassword=NULL, sessionStart=NULL WHERE username=?");
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPasswordHash(String username, String hash) {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE user_profiles SET hashedPassword=? WHERE username=?");
            ps.setString(1, hash);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPasswordHash(String username) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT hashedPassword FROM user_profiles WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("hashedPassword");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setSessionStart(String username) {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE user_profiles SET sessionStart=? WHERE username=?");
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Timestamp getSessionStart(String username) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT sessionStart FROM user_profiles WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp("sessionStart");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setLastAddress(String username, String address) {
        try {
            PreparedStatement ps = connection.prepareStatement("UPDATE user_profiles SET lastAddress=? WHERE username=?");
            ps.setString(1, address);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getLastAddress(String username) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT lastAddress FROM user_profiles WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("lastAddress");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean getPremium(String username) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT premium FROM user_profiles WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("premium");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean exists(String username) {
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM user_profiles WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return (connection != null);
    }

    public Connection getConnection() {
        return connection;
    }
}
