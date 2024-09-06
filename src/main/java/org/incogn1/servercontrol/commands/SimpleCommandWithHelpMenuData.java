package org.incogn1.servercontrol.commands;

import com.velocitypowered.api.command.SimpleCommand;
import org.jetbrains.annotations.NotNull;

public interface SimpleCommandWithHelpMenuData extends SimpleCommand {
    @NotNull
    HelpMenuData getHelpMenuData();
}
