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

package tk.blitzfarm.blitzlogin.velocity.commands;

import com.google.common.hash.Hashing;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.proxy.Player;
import tk.blitzfarm.blitzlogin.velocity.BlitzLogin;
import tk.blitzfarm.blitzlogin.velocity.listeners.PlayerAuthHandler;
import tk.blitzfarm.blitzlogin.velocity.listeners.ProxyConnectListener;
import tk.blitzfarm.blitzlogin.velocity.util.ConfigUtil;
import tk.blitzfarm.blitzlogin.velocity.util.DatabaseUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static tk.blitzfarm.blitzlogin.velocity.BlitzLogin.AUTHENTICATED;

public final class LoginCommand implements RawCommand {
    private final BlitzLogin plugin;
    private final ConfigUtil config;
    private final DatabaseUtil sql;
    private static final HashMap<String,Integer> TRIES = new HashMap<>();

    public LoginCommand(BlitzLogin plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
        sql = plugin.getSql();
    }

    @Override
    public void execute(final Invocation i) {
        // check if the command was executed by a player
        if (!(i.source() instanceof Player)) return;
        Player p = (Player) i.source();
        String username = p.getUsername();
        // check if the player is a premium user
        if (p.isOnlineMode()) {
            p.sendMessage(Component.text("Premium users do not need to login", NamedTextColor.GOLD));
            return;
        }
        // check if the player is already logged in
        if (AUTHENTICATED.contains(username)) {
            p.sendMessage(Component.text("You are already logged in", NamedTextColor.RED));
            return;
        }
        // check if a password as provided
        if (i.arguments().isEmpty()) {
            p.sendMessage(Component.text("Usage: /login <password>", NamedTextColor.RED));
            return;
        }
        // check if the player is registered
        if (sql.getPasswordHash(username) == null) {
            p.sendMessage(Component.text("You are not registered", NamedTextColor.RED));
            return;
        }
        // check if the password is correct
        if (Hashing.sha256().hashString(i.arguments(), StandardCharsets.UTF_8).toString().equals(sql.getPasswordHash(username))) {
            // authenticate the player
            AUTHENTICATED.add(username);
            sql.setSessionStart(username);
            sql.setLastAddress(username, p.getRemoteAddress().getHostName());
            // cancel the kick task
            PlayerAuthHandler.cancelTask(username);
            // send plugin message
            plugin.send(p.getCurrentServer().get(), "add", username);
            p.sendMessage(Component.text("Logged in successfully", NamedTextColor.GREEN));
            // send the player to the first main server (unless they are already connected to it)
            try {
                if (config.getMainServers().contains(p.getCurrentServer().get().getServerInfo().getName())) return;
                p.createConnectionRequest(plugin.getProxy().getServer(config.getMainServers().get(0)).get()).fireAndForget();
            } catch (ObjectMappingException e) {
                e.printStackTrace();
            }
        } else {
            setCooldown(p, config);
        }
    }

    static void setCooldown(Player p, ConfigUtil config) {
        String username = p.getUsername();
        TRIES.putIfAbsent(username, 0);
        TRIES.replace(username, TRIES.get(username) + 1);
        p.sendMessage(Component.text("Wrong password! You have "
                + (config.getMaxLoginAttempts() - TRIES.get(username)) + " attempts remaining", NamedTextColor.RED));
        if (TRIES.get(username) >= config.getMaxLoginAttempts()) {
            ProxyConnectListener.COOLDOWN.put(p.getRemoteAddress().getHostName(), System.currentTimeMillis());
            p.disconnect(Component.text("All connections from this IP address will be denied for the next " + config.getCooldownTime() +
                    " seconds\ndue to attempting to log in with the wrong password too many times", NamedTextColor.RED));
            TRIES.remove(username);
        }
    }
}
