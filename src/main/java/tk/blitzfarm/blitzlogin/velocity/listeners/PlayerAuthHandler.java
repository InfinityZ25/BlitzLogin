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
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.scheduler.ScheduledTask;
import tk.blitzfarm.blitzlogin.velocity.BlitzLogin;
import tk.blitzfarm.blitzlogin.velocity.util.ConfigUtil;
import tk.blitzfarm.blitzlogin.velocity.util.DatabaseUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static tk.blitzfarm.blitzlogin.velocity.BlitzLogin.AUTHENTICATED;

public class PlayerAuthHandler {
    private final BlitzLogin plugin;
    private final DatabaseUtil sql;
    private final ConfigUtil config;
    private final static HashMap<String, CombinedTasks> kickTask = new HashMap<>();

    public PlayerAuthHandler(final BlitzLogin plugin) {
        this.plugin = plugin;
        sql = plugin.getSql();
        config = plugin.getConfig();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(final LoginEvent e) {
        Player p = e.getPlayer();
        String username = p.getUsername();
        String ip = p.getRemoteAddress().getHostName();
        // authorize premium players
        if (p.isOnlineMode()) {
            AUTHENTICATED.add(username);
            sql.setLastAddress(username, ip);
            return;
        }
        // check if the session is valid
        if (sql.getSessionStart(username) != null) {
            if (System.currentTimeMillis() - sql.getSessionStart(username).getTime() <= config.getSessionTime() * 60000
                    && (ip.equals(sql.getLastAddress(username)))) {
                AUTHENTICATED.add(username);
            }
        }
    }

    // tell the backend limbo that the player is authorized
    @Subscribe
    public void onLimboServerPostConnect(final ServerPostConnectEvent e) throws ObjectMappingException {
        Player p = e.getPlayer();
        if (!p.getCurrentServer().isPresent()) return;
        ServerConnection server = p.getCurrentServer().get();
        if (!config.getLimboServers().contains(server.getServerInfo().getName())) return;
        if (AUTHENTICATED.contains(p.getUsername())) {
            plugin.send(server, "add", p.getUsername());
        }
    }

    @Subscribe
    public void onServerConnect(final ServerConnectedEvent e) {
        Player p = e.getPlayer();
        // check if the player is already authenticated
        if (AUTHENTICATED.contains(p.getUsername())) return;
        createTask(p, plugin);
    }

    @Subscribe
    public void onDisconnect(final DisconnectEvent e) {
        Player p = e.getPlayer();
        String username = p.getUsername();
        cancelTask(username);
        // refresh the session
        if (config.getRefreshSession() && AUTHENTICATED.contains(username) && !p.isOnlineMode()) {
            sql.setLastAddress(username, p.getRemoteAddress().getHostName());
            sql.setSessionStart(username);
        }
        AUTHENTICATED.remove(username);
    }

    public static void cancelTask(String username) {
        if (kickTask.containsKey(username)) {
            kickTask.get(username).cancel();
            kickTask.remove(username);
        }
    }

    private static class CombinedTasks {
        private final ScheduledTask scheduled;
        private final Runnable removeAuthBar;
        public CombinedTasks(ScheduledTask scheduled, Runnable removeAuthBar) {
            this.scheduled = scheduled;
            this.removeAuthBar = removeAuthBar;
        }
        public void cancel() {
            scheduled.cancel();
            removeAuthBar.run();
        }
    }

    public static void createTask(Player player, BlitzLogin plugin) {
        int maxAuthTime = plugin.getConfig().getMaxAuthTime();
        String username = player.getUsername();
        // read the max auth time from the config
        AtomicInteger time = new AtomicInteger(maxAuthTime);
        // create and show the boss bar
        BossBar authBar = BossBar.bossBar(Component.text("Remaining time to authenticate: " + time, NamedTextColor.GOLD, TextDecoration.BOLD),
                time.floatValue()/maxAuthTime, BossBar.Color.RED, BossBar.Overlay.NOTCHED_12);
        player.showBossBar(authBar);
        time.getAndIncrement();
        kickTask.putIfAbsent(username, new CombinedTasks(plugin.getProxy().getScheduler()
                .buildTask(plugin, () -> {
                    // decrease the time
                    time.getAndDecrement();
                    authBar.progress(time.floatValue()/maxAuthTime);
                    authBar.name(Component.text("Remaining time to authenticate: " + time, NamedTextColor.GOLD, TextDecoration.BOLD));
                    // once the timer hits 0 the player is kicked and the task is cancelled
                    if (time.get() <= 0) {
                        player.disconnect(Component.text("You did not register or login in time", NamedTextColor.RED));
                        cancelTask(username);
                    }
                }).repeat(1L, TimeUnit.SECONDS).schedule(), () -> player.hideBossBar(authBar)));
    }
}
