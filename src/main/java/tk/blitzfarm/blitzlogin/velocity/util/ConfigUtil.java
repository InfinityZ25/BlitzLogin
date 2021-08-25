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

package tk.blitzfarm.blitzlogin.velocity.util;

import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

public class ConfigUtil {

    private static final File configFile = new File("plugins/blitzlogin", "config.yml");
    private static final YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder().setFile(configFile).setIndent(2).setFlowStyle(DumperOptions.FlowStyle.BLOCK).build();
    private final ConfigurationNode config = loader.load();
    private final ConfigurationNode defaultConfig = YAMLConfigurationLoader.builder().setURL(this.getClass().getClassLoader().getResource("velocity/config.yml")).build().load();

    public ConfigUtil() throws IOException {
    }

    public ConfigurationNode getConfig() {
        return config;
    }

    public String getStorageAddress() {
        checkNode("storageAddress");
        return config.getNode("storageAddress").getString();
    }

    public String getStorageDatabase() {
        checkNode("storageDatabase");
        return config.getNode("storageDatabase").getString();
    }

    public String getStorageUsername() {
        checkNode("storageUsername");
        return config.getNode("storageUsername").getString();
    }

    public String getStoragePassword() {
        checkNode("storagePassword");
        return config.getNode("storagePassword").getString();
    }

    public Integer getSecondAttempt() {
        checkNode("secondAttempt");
        return config.getNode("secondAttempt").getInt();
    }

    public List<String> getLimboServers() throws ObjectMappingException {
        checkNode("limboServers");
        return config.getNode("limboServers").getList(TypeToken.of(String.class));
    }

    public List<String> getMainServers() throws ObjectMappingException {
        checkNode("mainServers");
        return config.getNode("mainServers").getList(TypeToken.of(String.class));
    }

    public Integer getMaxAuthTime() {
        checkNode("maxAuthTime");
        return config.getNode("maxAuthTime").getInt();
    }

    public Integer getMaxLoginAttempts() {
        checkNode("maxLoginAttempts");
        return config.getNode("maxLoginAttempts").getInt();
    }

    public Integer getCooldownTime() {
        checkNode("cooldownTime");
        return config.getNode("cooldownTime").getInt();
    }

    public Integer getSessionTime() {
        checkNode("sessionTime");
        return config.getNode("sessionTime").getInt();
    }

    public Boolean getRefreshSession() {
        checkNode("refreshSession");
        return config.getNode("refreshSession").getBoolean();
    }

    public Boolean getLogPremium() {
        checkNode("logPremium");
        return config.getNode("logPremium").getBoolean();
    }

    public Boolean getLogCracked() {
        checkNode("logCracked");
        return config.getNode("logCracked").getBoolean();
    }

    // put the default config value if the node is empty
    private void checkNode(String node) {
        if (config.getNode(node).isEmpty()) {
            config.getNode(node).setValue(defaultConfig.getNode(node));
            try {
                loader.save(config);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // generate config file if it doesn't exist
    public boolean checkFile() throws IOException {
        if (!configFile.exists()) {
            configFile.mkdir();
            if (!configFile.exists()) {
                try (InputStream in = this.getClass().getClassLoader().getResourceAsStream("velocity/config.yml")) {
                    Files.copy(in, configFile.toPath());
                }
            }
            return false;
        }
        return true;
    }
}
