package org.incogn1.servercontrol.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.incogn1.servercontrol.commands.SimpleCommandWithHelpMenuData;
import org.incogn1.servercontrol.commands.HelpMenuData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.incogn1.servercontrol.ServerControl.*;
import static org.incogn1.servercontrol.resources.MinecraftFontCalculator.*;

/**
 * Command: help
 * <p>
 * Shows the help menu.
 */
public class HelpCommand implements SimpleCommandWithHelpMenuData {

    private final Map<String, SimpleCommandWithHelpMenuData> commands;

    public HelpCommand(Map<String, SimpleCommandWithHelpMenuData> commands) {
        this.commands = commands;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        String command = null;
        int pageIndex = 0;

        // Page or command argument
        if (args.length >= 2) {
            String arg = args[1];

            try {
                pageIndex = Integer.parseInt(arg) - 1;
            } catch (NumberFormatException e) {
                if (commands.containsKey(arg)) {
                    command = arg;
                } else {
                    source.sendMessage(
                        translationsManager.translateAsMiniMessage(
                            "commands.help.invalid_page_or_command_arg",
                            Map.of(
                                "arg", arg
                            )
                        )
                    );

                    return;
                }
            }
        }

        // Page argument
        if (args.length >= 3) {
            String arg = args[2];

            try {
                pageIndex = Integer.parseInt(arg) - 1;
            } catch (NumberFormatException e) {
                source.sendMessage(
                    translationsManager.translateAsMiniMessage(
                        "commands.help.invalid_page_arg",
                        Map.of(
                            "arg", arg
                        )
                    )
                );

                return;
            }
        }

        source.sendMessage(
            genHelpMenu(command, pageIndex)
        );
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("servercontrol.base");
    }

    @Override
    public @NotNull HelpMenuData getHelpMenuData() {
        return new HelpMenuData(
            "Help",
            "/sc help [page|command] [page]",
            new String[]{ "page_or_command", "page" },
            new String[]{ "[page|command]", "[page]" }
        );
    }

    // -----
    //  Help menu generation code
    // -----

    private final int MENU_WIDTH_PX = 216;
    private final int MENU_HEIGHT_LINES = 20;
    private final int MENU_NAVIGATION_LINES_SCROLL = 5;

    private final int MENU_INDENT_WIDTH_PX = CharacterDetails.SPACE.getWidth();
    private final char MENU_INDENT_CHAR = CharacterDetails.SPACE.getCharacter();

    private final int MENU_HEADER_HEIGHT_LINES = 1;
    private final int MENU_FOOTER_HEIGHT_LINES = 1;

    private final int MENU_CONTENT_WIDTH_PX = MENU_WIDTH_PX - 2 * MENU_INDENT_WIDTH_PX;
    private final int MENU_CONTENT_HEIGHT_LINES = MENU_HEIGHT_LINES - MENU_HEADER_HEIGHT_LINES - MENU_FOOTER_HEIGHT_LINES;

    /**
     * Generates the help menu.
     *
     * @param command the command for which to generate the
     *      help menu. If null, the main help menu page will
     *      be generated.
     * @param pageIndex the index of the page to generate
     * @return a Component containing the help menu, to be
     *      sent directly to the player.
     */
    private Component genHelpMenu(String command, int pageIndex) {
        boolean isCommandMenu = command != null;

        // Generate full menu content
        Component[] menuContent;
        if (isCommandMenu) {
            menuContent = genCommandMenuContent(command);
        } else {
            menuContent = genMainMenuContent();
        }

        int pagesAmount = getPagesAmount(menuContent);

        // Contain pageIndex within possible margins
        if (pageIndex < 0) {
            pageIndex = 0;
        } else if (pageIndex >= pagesAmount) {
            pageIndex = pagesAmount - 1;
        }

        // Get page content for given index
        Component header = genHeader();
        Component[] pageContent = getPageContentByIndex(menuContent, pageIndex);
        Component footer = genFooter(
            pagesAmount,
            pageIndex,
            isCommandMenu ? "/sc help " + command : "/sc help",
            isCommandMenu
        );

        // Generate page
        Component page = header.append(Component.newline());

        for (Component line : pageContent) {
            if (line == null) {
                line = Component.empty();
            }
            page = page
                .append(Component.text(MENU_INDENT_CHAR))
                .append(line)
                .append(Component.newline());
        }

        page = page.append(footer);

        return page;
    }

    /**
     * Generates the header for a help menu page.
     *
     * @return a Component to be used as header
     */
    private Component genHeader() {
        return MiniMessage.miniMessage().deserialize(
                "<gray>-----</gray> <b>ServerControl help menu</b> <gray>-----</gray>"
        );
    }

    /**
     * Generates the footer for a help menu page.
     *
     * @param pages the amount of pages in the menu
     * @param currentPage the index of the currently shown page
     * @param baseNavCommand the command, without the index argument,
     *      that will be used to navigate through the pages.
     * @param showBackButton whether to show a button leading
     *      back to the main menu.
     * @return a Component to be used as footer.
     */
    private Component genFooter(int pages, int currentPage, String baseNavCommand, boolean showBackButton) {
        String backCommand = showBackButton ? " <click:run_command:'/sc help'><white><--</white></click> " : "----";
        String prevCommand = currentPage > 0 ? "<click:run_command:'" + baseNavCommand + " " + currentPage + "'><white><<</white></click>" : "<<";
        String nextCommand = currentPage + 1 < pages ? "<click:run_command:'" + baseNavCommand + " " + (currentPage + 2) + "'><white>>></white></click>" : ">>";
        String navCommands = pages > 1 ? " " + prevCommand + " " + (currentPage + 1) + " / " + pages + " " + nextCommand + " " : "----------";

        // Possible formats:
        // !showBackButton && !pages > 1 | ------------------------------------
        // showBackButton && !pages > 1  | -- <-- ------------------------------
        // !showBackButton && pages > 1  | ------------- << 1 / 2 >> -------------
        // showBackButton && pages > 1   | -- <-- ------- << 1 / 2 >> -------------

        return MiniMessage.miniMessage().deserialize(
            "<gray>--" + backCommand + "-------" + navCommands + "-------------</gray>"
        );
    }

    /**
     * Calculates the amount of pages the given menu content
     * would span across.
     *
     * @param menuContent the full menu content
     * @return the minimal amount of pages needed to show the
     *         full menu content.
     */
    private int getPagesAmount(Component[] menuContent) {
        int pages = 1;
        int currentPageEnd = MENU_CONTENT_HEIGHT_LINES;
        while (currentPageEnd < menuContent.length) {
            currentPageEnd += MENU_NAVIGATION_LINES_SCROLL;
            pages++;
        }

        return pages;
    }

    /**
     * Returns the subset of the given menu content that
     * represents the page at the requested pageIndex.
     *
     * @param menuContent the full menu content
     * @param pageIndex the index of the page to be returned
     * @return an array of Components representing the lines
     *         of text for the page.
     */
    private Component[] getPageContentByIndex(Component[] menuContent, int pageIndex) {
        int pageContentStartIndex = pageIndex * MENU_NAVIGATION_LINES_SCROLL;
        int pageContentEndIndex = pageContentStartIndex + MENU_CONTENT_HEIGHT_LINES;

        // If it's the last page, don't show unnecessary blank lines
        if (pageContentEndIndex > menuContent.length) {
            pageContentStartIndex = Math.max(0, menuContent.length - MENU_CONTENT_HEIGHT_LINES);
            pageContentEndIndex = pageContentStartIndex + MENU_CONTENT_HEIGHT_LINES;
        }

        return Arrays.copyOfRange(menuContent, pageContentStartIndex, pageContentEndIndex);
    }

    /**
     * Generates the content for the main help menu.
     *
     * @return an array of Components representing the lines
     *         of text for the help menu.
     */
    private Component[] genMainMenuContent() {
        List<Component> lines = new ArrayList<>();

        // Empty line
        lines.add(Component.empty());

        // Add "click for info" text
        String clickForInfoText = translationsManager.translate("help_menu.click_for_info");
        String[] clickForInfoText_split = splitTextByWidth(clickForInfoText, MENU_CONTENT_WIDTH_PX);
        for (String line : clickForInfoText_split) {
            lines.add(MiniMessage.miniMessage().deserialize("<gray><i>" + line + "</i></gray>"));
        }

        // Empty line
        lines.add(Component.empty());

        // Add list of commands
        for (Map.Entry<String, SimpleCommandWithHelpMenuData> entry : commands.entrySet()) {
            String commandId = entry.getKey();
            SimpleCommandWithHelpMenuData command = entry.getValue();

            HelpMenuData helpMenuData = command.getHelpMenuData();
            String template = helpMenuData.template();
            String formattedTemplate = "<click:run_command:'/sc help " + commandId + "'><aqua>"
                .concat(
                    template
                        .replace("/", "<gray>/</gray>")
                        .replace("[", "<gray>[</gray>")
                        .replace("]", "<gray>]</gray>")
                        .replace("|", "<gray>|</gray>")
                ).concat("</aqua></click>");

            lines.add(MiniMessage.miniMessage().deserialize(formattedTemplate));
        }

        // Empty line
        lines.add(Component.empty());

        return lines.toArray(new Component[0]);
    }

    /**
     * Generates the content for the help menu of a command.
     *
     * @param commandId the id of the command to generate the
     *                  menu content for.
     * @return an array of Components representing the lines
     *         of text for the help menu.
     */
    private Component[] genCommandMenuContent(String commandId) {
        List<Component> lines = new ArrayList<>();

        HelpMenuData helpMenuData = commands.get(commandId).getHelpMenuData();

        // Empty line
        lines.add(Component.empty());

        // Add command name
        String name = helpMenuData.name();
        lines.add(MiniMessage.miniMessage().deserialize(
            "<gray>" + translationsManager.translate("common.command") + " - </gray>" + "<aqua>" + name + "</aqua>"
        ));

        // Add command usage
        String usage = helpMenuData.template();
        String formattedUsage = "<aqua>"
            .concat(
                usage
                    .replace("/", "<gray>/</gray>")
                    .replace("[", "<gray>[</gray>")
                    .replace("]", "<gray>]</gray>")
                    .replace("|", "<gray>|</gray>")
            ).concat("</aqua>");
        lines.add(MiniMessage.miniMessage().deserialize(
            "<gray>" + translationsManager.translate("common.usage") + ":"
        ));
        lines.add(MiniMessage.miniMessage().deserialize(formattedUsage));

        // Empty line
        lines.add(Component.empty());

        // Add command description
        lines.add(MiniMessage.miniMessage().deserialize(
            "<gray>" + translationsManager.translate("common.description") + ": </gray>"
        ));
        String description = translationsManager.translate("help_menu.command_descriptions." + commandId + ".main");
        String[] description_split = splitTextByWidth(description, MENU_CONTENT_WIDTH_PX);
        for (String line : description_split) {
            lines.add(Component.text(line));
        }

        // Empty line
        lines.add(Component.empty());

        // Add argument descriptions
        String[] argumentIds = helpMenuData.argumentIds();
        String[] argumentTemplates = helpMenuData.argumentTemplates();
        for (int i = 0; i < argumentIds.length; i++) {
            String argumentId = argumentIds[i];
            String argumentTemplate = argumentTemplates[i];

            // Add argument description header
            lines.add(MiniMessage.miniMessage().deserialize(
                "<gray>" + translationsManager.translate("common.argument") + " - " + argumentTemplate + ":</gray>"
            ));

            // Add argument description
            String argumentDescription = translationsManager.translate("help_menu.command_descriptions." + commandId + ".arguments." + argumentId);
            String[] argumentDescription_split = splitTextByWidth(argumentDescription, MENU_CONTENT_WIDTH_PX);
            for (String line : argumentDescription_split) {
                lines.add(Component.text(line));
            }

            // Empty line between arguments
            if (i < argumentIds.length - 1) {
                lines.add(Component.empty());
            }
        }

        // Empty line
        lines.add(Component.empty());

        return lines.toArray(new Component[0]);
    }
}
