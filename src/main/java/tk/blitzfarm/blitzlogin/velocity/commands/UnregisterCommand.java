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
import tk.blitzfarm.blitzlogin.velocity.listeners.PlayerAuthHandler;

import java.nio.charset.StandardCharsets;

public final class UnregisterCommand implements RawCommand {
    private final BlitzLogin plugin;
    private final ConfigUtil config;
    private final DatabaseUtil sql;

    public UnregisterCommand(BlitzLogin plugin) {
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
            i.source().sendMessage(Component.text("Premium users cannot unregister", NamedTextColor.GOLD));
            return;
        }
        // check if a password as provided
        if (i.arguments().isEmpty()) {
            i.source().sendMessage(Component.text("Usage: /unregister <password>", NamedTextColor.RED));
            return;
        }
        // check if the password is correct
        if (Hashing.sha256().hashString(i.arguments(), StandardCharsets.UTF_8).toString().equals(sql.getPasswordHash(username))) {
            BlitzLogin.AUTHENTICATED.remove(username);
            sql.unregisterPlayer(username);
            p.sendMessage(Component.text("Unregistered successfully", NamedTextColor.GREEN));
            try {
                if (config.getLimboServers().contains(p.getCurrentServer().get().getServerInfo().getName())) {
                    plugin.send(p.getCurrentServer().get(), "remove", username);
                    PlayerAuthHandler.createTask(p, plugin);
                    return;
                }
                p.createConnectionRequest(plugin.getProxy().getServer(config.getLimboServers().get(0)).get()).fireAndForget();
            } catch (ObjectMappingException e) {
                e.printStackTrace();
            }
        } else {
            LoginCommand.setCooldown(p, config);
        }
    }
}
