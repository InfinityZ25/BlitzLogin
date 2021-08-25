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
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent.DisconnectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import tk.blitzfarm.blitzlogin.velocity.BlitzLogin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.util.List;

public class PlayerServerHandler {

    private final BlitzLogin plugin;
    private final List<String> limboServers;
    private final List<String> mainServers;
    private static int limboIndex = 0;
    private static int mainIndex = 0;

    public PlayerServerHandler(final BlitzLogin plugin) throws ObjectMappingException {
        this.plugin = plugin;
        limboServers = plugin.getConfig().getLimboServers();
        mainServers = plugin.getConfig().getMainServers();
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerChooseInitialServer(final PlayerChooseInitialServerEvent e) {
        String username = e.getPlayer().getUsername();
        // check if the player is premium/authorized or unauthorized
        if (plugin.getSql().getPremium(username) || BlitzLogin.AUTHENTICATED.contains(username)) {
            e.setInitialServer(getServer(mainServers, 0));
        } else {
            e.setInitialServer(getServer(limboServers, 0));
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onServerKick(final KickedFromServerEvent e) {
        Player p = e.getPlayer();

        // check if the player is authorized
        if (BlitzLogin.AUTHENTICATED.contains(p.getUsername())) {
            // ensure players aren't accidentally kicked
            // check if the player wanted to connect to an offline limbo server
            if (p.getCurrentServer().isPresent()) return;
            mainIndex++;
            // check if there are any other main servers to choose from
            if (mainIndex == mainServers.size()) {
                e.setResult(DisconnectPlayer.create(Component.text("Unable to connect to a main server",NamedTextColor.RED)));
                mainIndex = 0;
                return;
            }
            // redirect the player to the next main server in the list
            e.setResult(RedirectPlayer.create(getServer(mainServers, mainIndex),
                    Component.text("You have been redirected to a fallback server",NamedTextColor.RED)));
        } else {
            limboIndex++;
            // check if there are any other limbo servers to choose from
            if (limboIndex == limboServers.size()) {
                e.setResult(DisconnectPlayer.create(Component.text("Unable to connect to a limbo server",NamedTextColor.RED)));
                limboIndex = 0;
                return;
            }
            // redirect the player to the next limbo server in the list
            e.setResult(RedirectPlayer.create(getServer(limboServers, limboIndex),
                    Component.text("You have been redirected to a fallback server",NamedTextColor.RED)));
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerConnect(final ServerConnectedEvent e) {
        Player p = e.getPlayer();
        RegisteredServer s = e.getServer();

        // ensure unauthorized players don't end up in non limbo servers
        if (!BlitzLogin.AUTHENTICATED.contains(p.getUsername()) && !limboServers.contains(s.getServerInfo().getName())) {
            p.disconnect(Component.text("You were unexpectedly moved to a non limbo server so you have been kicked",NamedTextColor.RED));
        }

        // reset the index on player connect
        if (s.equals(getServer(mainServers, mainIndex))) {
            mainIndex = 0;
        }
        if (s.equals(getServer(limboServers, limboIndex))) {
            limboIndex = 0;
        }
    }

    private RegisteredServer getServer(List<String> serverList, int index) {
        return plugin.getProxy().getServer(serverList.get(index)).get();
    }
}
