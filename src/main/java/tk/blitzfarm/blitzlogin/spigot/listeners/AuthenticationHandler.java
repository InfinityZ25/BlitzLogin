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

package tk.blitzfarm.blitzlogin.spigot.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import tk.blitzfarm.blitzlogin.spigot.BlitzLoginSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.HashMap;

import static tk.blitzfarm.blitzlogin.spigot.BlitzLoginSpigot.AUTHENTICATED;

public class AuthenticationHandler implements PluginMessageListener, Listener {
    private final BlitzLoginSpigot plugin;
    public static final HashMap<String, PlayerEffects> EFFECTS = new HashMap<>();

    public AuthenticationHandler(BlitzLoginSpigot plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("blitzlogin:auth")) return;
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        String user = in.readUTF();
        switch (subchannel) {
            case "add":
                AUTHENTICATED.add(user);
                EFFECTS.get(user).restore();
                EFFECTS.remove(user);
                player.sendMessage("You are authorized");
                System.out.println("received");
                break;
            case "remove":
                Player p = Bukkit.getPlayer(user);
                AUTHENTICATED.remove(user);
                setPlayerProperties(p);
                break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        setPlayerProperties(p);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        String user = e.getPlayer().getName();
        if (!EFFECTS.containsKey(user)) return;
        EFFECTS.get(user).restore();
        EFFECTS.remove(user);
        AUTHENTICATED.remove(user);
    }

    static class PlayerEffects {
        private final Player player;
        private final boolean fly;
        private final float speed;
        private final int air;
        private final int fire;
        private final boolean pickup;
        private final ItemStack[] inventory;
        private final Collection<PotionEffect> potion;
        public PlayerEffects(Player player) {
            this.player = player;
            this.fly = player.getAllowFlight();
            this.speed = player.getWalkSpeed();
            this.air = player.getRemainingAir();
            this.fire = player.getFireTicks();
            this.pickup = player.getCanPickupItems();
            this.inventory = player.getInventory().getContents();
            this.potion = player.getActivePotionEffects();
        }
        public void restore() {
            player.setAllowFlight(fly);
            player.setWalkSpeed(speed);
            player.setFireTicks(fire);
            player.setCanPickupItems(pickup);
            player.setRemainingAir(air);
            player.getInventory().setContents(inventory);
            player.updateInventory();
            player.addPotionEffects(potion);
        }
    }

    private void setPlayerProperties(Player p) {
        EFFECTS.put(p.getName(), new PlayerEffects(p));
        if (plugin.HIDE_INVENTORY) {
            p.setCanPickupItems(false);
            p.getInventory().clear();
            p.updateInventory();
        }
        if (!plugin.MOVEMENT) {
            p.setWalkSpeed(0);
            p.setAllowFlight(true);
        }
        if (plugin.PRESERVE_EFFECTS) {
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
        }
    }
}
