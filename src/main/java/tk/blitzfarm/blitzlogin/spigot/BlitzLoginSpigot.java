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

package tk.blitzfarm.blitzlogin.spigot;

import tk.blitzfarm.blitzlogin.spigot.listeners.AuthenticationHandler;
import tk.blitzfarm.blitzlogin.spigot.listeners.PlayerActionHandler;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public final class BlitzLoginSpigot extends JavaPlugin {

    private static FileConfiguration customConfig;
    public static final Set<String> AUTHENTICATED = new HashSet<>();
    public boolean FIXED_SPAWN;
    public boolean HIDE_UNAUTHENTICATED;
    public boolean MOVEMENT;
    public boolean HIDE_INVENTORY;
    public boolean INTERACT;
    public boolean PRESERVE_EFFECTS;

    @Override
    public FileConfiguration getConfig() {
        return customConfig;
    }

    @Override
    public void onEnable() {
        createCustomConfig();
        FIXED_SPAWN = customConfig.getBoolean("fixedSpawn");
        HIDE_UNAUTHENTICATED = customConfig.getBoolean("hideUnauthorized");
        INTERACT = customConfig.getBoolean("allowInteract");
        MOVEMENT = customConfig.getBoolean("allowMovement");
        HIDE_INVENTORY = customConfig.getBoolean("hideInventory");
        PRESERVE_EFFECTS = customConfig.getBoolean("preserveEffects");
        // register listeners
        getServer().getPluginManager().registerEvents(new PlayerActionHandler(this), this);
        getServer().getPluginManager().registerEvents(new AuthenticationHandler(this), this);

        // register the channel
        getServer().getMessenger().registerIncomingPluginChannel(this, "blitzlogin:auth", new AuthenticationHandler(this));

//        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () ->
//                getServer().getConsoleSender().sendMessage(AUTHENTICATED.toString()),
//                0L, 40L);

        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "Plugin BlitzLogin enabled");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.RED + "Plugin BlitzLogin disabled");
    }


    private void createCustomConfig() {
        File customConfigFile = new File(getDataFolder(), "config.yml");
        if (!customConfigFile.exists()) {
            customConfigFile.getParentFile().mkdirs();
            try {
                Files.copy(getResource("spigot/config.yml"), customConfigFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
        try {
            customConfig.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}