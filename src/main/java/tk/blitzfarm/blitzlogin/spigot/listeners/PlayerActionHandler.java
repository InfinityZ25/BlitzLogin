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

import tk.blitzfarm.blitzlogin.spigot.BlitzLoginSpigot;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.*;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import static tk.blitzfarm.blitzlogin.spigot.BlitzLoginSpigot.AUTHENTICATED;

public class PlayerActionHandler implements Listener {
    private final BlitzLoginSpigot plugin;
    private final Location location;

    public PlayerActionHandler(BlitzLoginSpigot plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();

        location = new Location(plugin.getServer().getWorld(
                config.getString("spawnWorld")),
                config.getInt("spawnX") + 0.5,
                config.getInt("spawnY"),
                config.getInt("spawnX") + 0.5,
                (float) config.getDouble("spawnYaw"),
                (float) config.getDouble("spawnPitch"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawn(PlayerSpawnLocationEvent e) {
        if (!plugin.FIXED_SPAWN) return;
        e.setSpawnLocation(location);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (plugin.MOVEMENT || isAuthed(e)) return;
        double fromX = e.getFrom().getX();
        double fromY = e.getFrom().getY();
        double fromZ = e.getFrom().getZ();
        double toX = e.getTo().getX();
        double toY = e.getTo().getY();
        double toZ = e.getTo().getZ();

        if (fromX != toX || fromY != toY || fromZ != toZ)
            e.setTo(e.getFrom());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent e) {
        if (interactAllowed(e.getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockInteract(PlayerInteractEvent e) {
        if (interactAllowed(e.getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (interactAllowed(((Player) e.getDamager()).getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDamaged(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (interactAllowed(((Player) e.getEntity()).getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (interactAllowed(e.getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(PlayerPickupItemEvent e) {
        if (interactAllowed(e.getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent e) {
        if (interactAllowed(e.getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTargeted(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player)) return;
        if (interactAllowed(((Player) e.getTarget()).getPlayer())) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (isAuthed(e)) return;
        e.setCancelled(true);
    }


    private boolean interactAllowed(Player player) {
        return (plugin.INTERACT || AUTHENTICATED.contains(player.getName()));
    }
    private boolean isAuthed(PlayerEvent e) {
        return AUTHENTICATED.contains(e.getPlayer().getName());
    }
}