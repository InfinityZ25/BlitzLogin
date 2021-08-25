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

package tk.blitzfarm.blitzlogin.velocity.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import tk.blitzfarm.blitzlogin.velocity.BlitzLogin;
import tk.blitzfarm.blitzlogin.velocity.util.ConfigUtil;
import tk.blitzfarm.blitzlogin.velocity.util.DatabaseUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class ProxyConnectListener {

    private final BlitzLogin plugin;
    private final ConfigUtil config;
    private final DatabaseUtil sql;
    private static final HashMap<String, Boolean> failedConnecting = new HashMap<>();
    public static final HashMap<String, Long> COOLDOWN = new HashMap<>();

    public ProxyConnectListener(BlitzLogin plugin) {
        this.plugin = plugin;
        sql = plugin.getSql();
        config = plugin.getConfig();
    }

    @Subscribe
    public void onPreJoin(final PreLoginEvent e) {
        String username = e.getUsername();
        String ip = e.getConnection().getRemoteAddress().getHostName();
        // kick players that guessed the wrong password too many times
        if (COOLDOWN.containsKey(ip)) {
            // get the remaining cooldown time
            long time = config.getCooldownTime() - (System.currentTimeMillis() - COOLDOWN.get(ip)) / 1000;
            // delete the entry from the map if the cooldown expired
            if (time <= 0) {
                COOLDOWN.remove(ip);
                return;
            }
            // kick the player
            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component
                    .text("All connections from this IP address will be denied for the next " + time +
                            " seconds\ndue to attempting to log in with the wrong password too many times", NamedTextColor.RED)));
        }

        // update HashMap on preLogin only if the player isn't in the database
        if (!sql.exists(username)) {
            failedConnecting.putIfAbsent(username, false);
        }

        // this handles new players
        // if the premium account with this name exists
        if (resolver(username).contains("name")) {
            // if the user profile is in the database
            if (sql.exists(username)) {
                // if the user profile is premium in the database
                if (sql.getPremium(username)) {
                    e.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                }
            } else {
                // first attempt
                if (!failedConnecting.get(username)) {
                    e.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
                }
            }
        }

        if (e.getResult().isOnlineModeAllowed()) {
            failedConnecting.replace(username, true);

            // waits the amount of seconds specified in the config for a cracked user with a premium username to rejoin
            plugin.getProxy().getScheduler()
                    .buildTask(plugin, () -> failedConnecting.replace(username, false))
                    .delay(config.getSecondAttempt(), TimeUnit.SECONDS)
                    .schedule();
        } else {
            failedConnecting.replace(username, false);
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChooseInitialServer(final PlayerChooseInitialServerEvent e) {
        String username = e.getPlayer().getUsername();

        // check if the player exists in the map
        if (failedConnecting.containsKey(username)) {
            // adds the player to the database
            sql.createPlayer(username, e.getPlayer().getUniqueId().toString(), failedConnecting.get(username));
            // remove the player from the HashMap as they are now stored in the database
            failedConnecting.remove(username);
        }

        // check if logging is enabled in the config
        if (sql.getPremium(username)) {
            if (config.getLogPremium()) plugin.getLogger().info(username + " logged in as premium");
        } else if (config.getLogCracked()) plugin.getLogger().info(username + " logged in as cracked");
    }


    // method for checking if the player profile is premium
    private static String resolver(String username) {
        StringBuilder sb = new StringBuilder();
        try {
            URLConnection urlConn = new URL("https://api.mojang.com/users/profiles/minecraft/" + username).openConnection();
            urlConn.setReadTimeout(60000);
            if (urlConn.getInputStream() != null) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), Charset.defaultCharset()));
                int cp;
                while ((cp = bufferedReader.read()) != -1) {
                    sb.append((char) cp);
                }
                bufferedReader.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Exception while calling URL", e);
        }
        return sb.toString();
    }
}
