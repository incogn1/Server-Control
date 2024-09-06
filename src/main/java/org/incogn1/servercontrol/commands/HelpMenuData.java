package org.incogn1.servercontrol.commands;

public record HelpMenuData(
        String name,
        String template,
        String[] argumentIds,
        String[] argumentTemplates) {
}
