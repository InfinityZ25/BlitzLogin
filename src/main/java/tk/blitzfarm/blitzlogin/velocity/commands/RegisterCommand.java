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
import tk.blitzfarm.blitzlogin.velocity.util.ConfigUtil;
import tk.blitzfarm.blitzlogin.velocity.util.DatabaseUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.nio.charset.StandardCharsets;

import static tk.blitzfarm.blitzlogin.velocity.BlitzLogin.AUTHENTICATED;
import static tk.blitzfarm.blitzlogin.velocity.listeners.PlayerAuthHandler.cancelTask;

public final class RegisterCommand implements RawCommand {
    private final BlitzLogin plugin;
    private final ConfigUtil config;
    private final DatabaseUtil sql;

    public RegisterCommand(BlitzLogin plugin) {
        this.plugin = plugin;
        config = plugin.getConfig();
        sql = plugin.getSql();
    }

    @Override
    public void execute(Invocation i) {
        String[] args = i.arguments().split(" ");
        // check if the command was executed by a player
        if (!(i.source() instanceof Player)) return;
        Player p = (Player) i.source();
        String username = p.getUsername();
        // check if the player is a premium user
        if (p.isOnlineMode()) {
            p.sendMessage(Component.text("Premium users do not need to register", NamedTextColor.GOLD));
            return;
        }
        // check if the player is already registered
        if (sql.getPasswordHash(username) != null) {
            p.sendMessage(Component.text("You are already registered", NamedTextColor.RED));
            return;
        }

        // check if the correct syntax was used
        if (args.length != 2) {
            p.sendMessage(Component.text("Usage: /register <password> <repeat-password>", NamedTextColor.RED));
        } else if (!args[0].equals(args[1])) {
            p.sendMessage(Component.text("Passwords must match", NamedTextColor.RED));
        } else {
            // set the player's password hash, session start and last IP address in the database
            sql.setPasswordHash(username, Hashing.sha256().hashString(args[0], StandardCharsets.UTF_8).toString());
            sql.setSessionStart(username);
            sql.setLastAddress(username, p.getRemoteAddress().getHostName());
            // add the player to the authorized list
            AUTHENTICATED.add(username);
            // cancel the kick task
            cancelTask(username);
            // send plugin message
            plugin.send(p.getCurrentServer().get(), "add", username);
            p.sendMessage(Component.text("Account successfully registered", NamedTextColor.GREEN));
            try {
                if (config.getMainServers().contains(p.getCurrentServer().get().getServerInfo().getName())) return;
                    p.createConnectionRequest(plugin.getProxy().getServer(config.getMainServers().get(0)).get()).fireAndForget();
            } catch (ObjectMappingException e) {
                e.printStackTrace();
            }
        }
    }
}
