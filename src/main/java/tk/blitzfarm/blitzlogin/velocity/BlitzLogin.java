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

package tk.blitzfarm.blitzlogin.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import tk.blitzfarm.blitzlogin.velocity.commands.LoginCommand;
import tk.blitzfarm.blitzlogin.velocity.commands.RegisterCommand;
import tk.blitzfarm.blitzlogin.velocity.commands.UnregisterCommand;
import tk.blitzfarm.blitzlogin.velocity.listeners.CommandListener;
import tk.blitzfarm.blitzlogin.velocity.listeners.PlayerAuthHandler;
import tk.blitzfarm.blitzlogin.velocity.listeners.PlayerServerHandler;
import tk.blitzfarm.blitzlogin.velocity.listeners.ProxyConnectListener;
import tk.blitzfarm.blitzlogin.velocity.util.ConfigUtil;
import tk.blitzfarm.blitzlogin.velocity.util.DatabaseUtil;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Plugin(
        id = "blitzlogin",
        name = "BlitzLogin",
        version = "@version@",
        authors = {"XXMA"}
)

public final class BlitzLogin {

    private final Logger logger;
    private final ProxyServer proxy;
    private final Path dataDirectory;
    private static ConfigUtil config;
    private static DatabaseUtil sql;
    public final static Set<String> AUTHENTICATED = new HashSet<>();
    private static final ChannelIdentifier AUTH_CHANNEL = MinecraftChannelIdentifier.create("blitzlogin", "auth");

    @Inject
    public BlitzLogin(Logger logger, ProxyServer proxy, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.proxy = proxy;
        this.dataDirectory = dataDirectory;
    }

    // getters
    public ProxyServer getProxy() {
        return proxy;
    }
    public Logger getLogger() {
        return logger;
    }
    public Path getDataDirectory() {
        return dataDirectory;
    }
    public ConfigUtil getConfig() {
        return config;
    }
    public DatabaseUtil getSql() {
        return sql;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) throws IOException, ObjectMappingException {
        JDBCDriver.inject(this);

        // disable the plugin if the config has just been generated
        config = new ConfigUtil();
        if (!config.checkFile()) {
            logger.info("It appears this is the first time you are running this plugin");
            logger.warn("You need to set the MySQL database connection details in the config.yml file");
            logger.warn("The plugin will be inactive");
            proxy.getEventManager().unregisterListeners(this);
            return;
        }

        try {
            sql = new DatabaseUtil(config.getStorageAddress(), config.getStorageDatabase(), config.getStorageUsername(), config.getStoragePassword());
        // disable plugin if the connection to the database cannot be established
        } catch (Exception ex) {
            if (config.getStorageAddress().isEmpty() || config.getStorageUsername().isEmpty() || config.getStoragePassword().isEmpty()) {
                logger.warn("You need to set the MySQL database connection details in the config.yml file");
                logger.warn("The plugin will be inactive");
            } else {
                ex.printStackTrace();
                logger.error("Database couldn't connect. Plugin will be inactive");
            }
            proxy.getEventManager().unregisterListeners(this);
            return;
        }

        // register the listeners
        proxy.getEventManager().register(this, new ProxyConnectListener(this));
        proxy.getEventManager().register(this, new PlayerServerHandler(this));
        proxy.getEventManager().register(this, new PlayerAuthHandler(this));
        proxy.getEventManager().register(this, new CommandListener(this));

        // register the plugin messaging channel
        proxy.getChannelRegistrar().register(AUTH_CHANNEL);

        // register the commands
        proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder("register").build(), new RegisterCommand(this));
        proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder("unregister").build(), new UnregisterCommand(this));
        proxy.getCommandManager().register(proxy.getCommandManager().metaBuilder("login").aliases("l").build(), new LoginCommand(this));

        logger.info("Plugin enabled");

    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        sql.disconnect();
        proxy.getEventManager().unregisterListeners(this);
        logger.info("Plugin disabled");
    }


    public void send(ChannelMessageSink receiver, String subchannel, String message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        out.writeUTF(message);
        receiver.sendPluginMessage(AUTH_CHANNEL, out.toByteArray());
    }
}
