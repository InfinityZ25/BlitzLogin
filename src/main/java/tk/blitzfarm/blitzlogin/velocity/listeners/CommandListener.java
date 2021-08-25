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

import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;
import tk.blitzfarm.blitzlogin.velocity.BlitzLogin;

import java.util.ArrayList;

import static tk.blitzfarm.blitzlogin.velocity.BlitzLogin.AUTHENTICATED;

public class CommandListener {
    private final BlitzLogin plugin;
    private final static ImmutableSet<String> allowedCommands = ImmutableSet.of("l", "login", "register", "unregister");

    public CommandListener(final BlitzLogin plugin) {
        this.plugin = plugin;
    }

    // cancel command execution for unauthenticated users
    @Subscribe
    public void onCommandExecute(final CommandExecuteEvent e) {
        if (!(e.getCommandSource() instanceof Player)) return;
        String username = ((Player) e.getCommandSource()).getUsername();
        // commands are enabled for premium and authenticated users
        if (AUTHENTICATED.contains(username)) return;
        // disable commands for unauthenticated users
        if (!allowedCommands.contains(e.getCommand().split(" ")[0])) {
            e.setResult(CommandExecuteEvent.CommandResult.denied());
        }
    }

    // hide commands for unauthenticated users (>1.13)
    @Subscribe
    public void onPlayerAvailableCommands(final PlayerAvailableCommandsEvent e) {
        if (AUTHENTICATED.contains(e.getPlayer().getUsername())) return;
        (new ArrayList<>(e.getRootNode().getChildren())).forEach((commandNode) -> {
            if (!allowedCommands.contains(commandNode.getName())) {
                e.getRootNode().removeChildByName(commandNode.getName());
            }
        });
    }

    // hide commands for unauthenticated users (<1.12)
    // NOT WORKING (event gets fired only on non-command)
    @Subscribe
    public void onTabComplete(final TabCompleteEvent e) {
        String username = e.getPlayer().getUsername();
        if (AUTHENTICATED.contains(username)) return;
        e.getSuggestions();
    }
}
