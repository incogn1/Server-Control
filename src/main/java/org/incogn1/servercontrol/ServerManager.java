package org.incogn1.servercontrol;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import dev.dejvokep.boostedyaml.route.Route;
import org.incogn1.servercontrol.scripts.MissingScriptException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.incogn1.servercontrol.ServerControl.*;

public class ServerManager {

    /** List of servers in boot-up process */
    private final Set<String> bootingServers = Collections.synchronizedSet(new HashSet<>());

    /** List of players waiting to join a specific server */
    private final Map<Player, String> waitingPlayers = Collections.synchronizedMap(new HashMap<>());

    /** List of sources listening for notifications about a specific server's boot-up process */
    private final Map<CommandSource, String> listeningSources = Collections.synchronizedMap(new HashMap<>());

    /**
     * Generates a map of all the RegisteredServer instances on the network
     * with their name as the key.
     *
     * @return a map of server name to RegisteredServer instance
     */
    public Map<String, RegisteredServer> getServers() {
        Map<String, RegisteredServer> servers = new HashMap<>();

        // Add all registered servers to map with their name as keys
        Collection<RegisteredServer> registeredServers = proxy.getAllServers();
        for (RegisteredServer server : registeredServers) {
            servers.put(server.getServerInfo().getName(), server);
        }

        return servers;
    }

    /**
     * Gets the RegisteredServer instance for the given name.
     *
     * @param serverName the name of the server
     * @return the RegisteredServer with the given name or null
     *         if no server with that name was found
     */
    public RegisteredServer getServer(String serverName) {
        Map<String, RegisteredServer> servers = getServers();

        return servers.get(serverName);
    }

    /**
     * Checks if the server is online by sending a ping.
     *
     * @param server the RegisteredServer instance to check
     * @return true if online, else false
     */
    public boolean getServerOnlineState(RegisteredServer server) {
        ServerPing ping = null;
        try {
            ping = server.ping().join();
        } catch (Exception ignore) {}

        return ping != null;
    }

    /**
     * Checks if the server is online by sending a ping.
     *
     * @param serverName the name of the server to check
     * @return true if online, else false
     */
    public boolean getServerOnlineState(String serverName) {
        RegisteredServer server = getServer(serverName);

        return getServerOnlineState(server);
    }

    /**
     * Checks if the server is currently booting up.
     *
     * @param server the RegisteredServer instance to check
     * @return true if booting, else false
     */
    public boolean getServerBootingState(RegisteredServer server) {
        String serverName = server.getServerInfo().getName();

        return getServerBootingState(serverName);
    }

    /**
     * Checks if the server is currently booting up.
     *
     * @param serverName the name of the server to check
     * @return true if booting, else false
     */
    public boolean getServerBootingState(String serverName) {
        return bootingServers.contains(serverName);
    }

    /**
     * Runs the startup script for the specified server. If the exit code of
     * the script is 0, players added to the queue with setDelayedPlayerJoin
     * will be connected to the server.
     *
     * @param serverName the name of the server to start
     * @return a CompletableFuture that returns the exit code of the script as an integer,
     *         or null if the server is already booting up
     *
     * @throws MissingScriptException if the startup script for the server does not exist
     */
    public CompletableFuture<Integer> startServer(String serverName) throws MissingScriptException, IOException {

        // Guard - Server should not already be booting up
        if (bootingServers.contains(serverName)) {
            return null;
        }

        // Determine the path to the script
        Path scriptPath = getServerStartupScriptPath(serverName);

        try {
            bootingServers.add(serverName);

            // Run startup script
            CompletableFuture<Integer> future = scriptManager.runScript(scriptPath);

            // Add method to handle exit code when script finishes executing
            future.thenAccept(exitCode -> {
                handleStartupScriptExitCode(exitCode, serverName);
                bootingServers.remove(serverName);
            });

            return future;
        } catch (MissingScriptException e) {
            bootingServers.remove(serverName);

            removeListeningSources(serverName);
            cancelDelayedJoins(serverName);

            throw e;
        } catch (IOException e) {
            bootingServers.remove(serverName);

            removeListeningSources(serverName);
            cancelDelayedJoins(serverName);

            // Send error message for debugging
            logger.error("Startup script for {} failed to execute due to IOException. See error details below. \n{}", serverName, e.getMessage());

            throw e;
        }
    }

    /**
     * Runs code depending on the exit code of a server startup script.
     * <p>
     * If the exit code is non-zero (e.g. an error occurred) the listening sources
     * are notified and delayed joins are cancelled.
     * <p>
     * If the exit code is 0, a polling process is started to detect when the
     * server comes online within the network. Once the server is online, listening
     * sources are notified and delayed joins are executed. However, if the server
     * has yet to startup after the timeout specified in the plugin's config has ended,
     * again listening sources are notified about this and delayed joins are cancelled.
     *
     * @param exitCode the exit code returned by the script
     * @param serverName the name of the server
     */
    public void handleStartupScriptExitCode(int exitCode, String serverName) {

        // ---
        // Case A - Non-successful script execution -> Notify user, cancel delayed joins & log exit code
        // ---
        if (exitCode != 0) {
            notifyListeningSources(serverName, false);
            removeListeningSources(serverName);
            cancelDelayedJoins(serverName);

            // Send error message for debugging
            logger.error("Startup script for {} failed with code {}.", serverName, exitCode);

            return;
        }

        // ---
        // Case B - Successful script execution -> Use polling to check when server has come online
        // ---

        // Get timeout from config
        Optional<Long> serverSpecificConfigTimeout = config.getOptionalLong(Route.from("server-startup", "server-specific-timeouts", serverName));
        long globalConfigTimeout = config.getLong(Route.from("server-startup", "global-timeout"));
        long timeoutConfigSeconds = serverSpecificConfigTimeout.orElse(globalConfigTimeout);
        long timeout = System.currentTimeMillis() + timeoutConfigSeconds * 1000;

        // Get polling delay from config
        long pollingDelay = config.getLong(Route.from("server-startup", "polling-delay")) * 1000;

        // Initiate polling process
        while (System.currentTimeMillis() < timeout) {

            // Server came online
            if (getServerOnlineState(serverName)) {
                notifyListeningSources(serverName, true);
                removeListeningSources(serverName);
                doDelayedJoins(serverName);

                return;
            }

            // Server not yet online
            try {
                Thread.sleep(pollingDelay);
            } catch (InterruptedException e) {
                // Send error message for debugging
                logger.error("The server startup polling process for server {} was momentarily interrupted. If you only see this message once, you can ignore it. Error: {}", serverName, e.getMessage());
            }
        }

        // Server did not come online within given timeout window
        notifyListeningSources(serverName, false);
        removeListeningSources(serverName);
        cancelDelayedJoins(serverName);

        // Send error message for debugging
        logger.error("The server {} took more than {} seconds to come online. Considering the startup process as a failure. If the server needs more time to start, consider changing the startup timeout for this server in the config file.", serverName, timeoutConfigSeconds);
    }

    /**
     * Container method for {@link #startServer(String)}. Functionality
     * is extended by notifying given source about booting process state.
     *
     * @param serverName the name of the server to start
     * @param source the CommandSource that should receive notifications
     */
    public void startServerWithNotify(String serverName, CommandSource source) {

        // Subscribe user to notifications
        setSourceNotifications(source, serverName);

        // Guard - Server should not already be booting up
        if (bootingServers.contains(serverName)) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "server_startup.starting",
                    Map.of(
                        "server", serverName
                    )
                )
            );

            return;
        }

        try {
            startServer(serverName);

            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "server_startup.starting",
                    Map.of(
                        "server", serverName
                    )
                )
            );
        } catch (MissingScriptException e) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "server_startup.no_script_defined",
                    Map.of(
                        "server", serverName
                    )
                )
            );
        } catch (IOException e) {
            source.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "server_startup.unknown_error",
                    Map.of(
                        "server", serverName
                    )
                )
            );
        }
    }

    /**
     * Returns the path to the startup script for the given server
     *
     * @param serverName the name of the server
     * @return a Path to the startup script relative to the
     *         'scripts' directory.
     */
    public Path getServerStartupScriptPath(String serverName) {
        String scriptFilePattern = config.getString(Route.from("server-startup", "script-pattern"));
        String scriptFile = scriptFilePattern.replaceAll("%server%", serverName);

        return Paths.get(SCRIPTS_DIR, scriptFile);
    }

    /**
     * Adds a player to the delayed join list of the specified server.
     * When the server is successfully started using startServer(), the
     * player will automatically be connected to that server.
     * <p>
     * If the player was already in a delayed join list for a different
     * server, that entry will be removed.
     * <p>
     * The player is notified if they are added to the delayed join list.
     *
     * @param player the player to be added to the queue
     * @param serverName the name of the server to queue for
     */
    public void setDelayedPlayerJoin(Player player, String serverName) {
        if (!Objects.equals(waitingPlayers.get(player), serverName)) {
            player.sendMessage(
                translationsManager.translateAsMiniMessage(
                    "server_startup.added_to_join_list",
                    Map.of(
                        "server", serverName
                    )
                )
            );
        }

        waitingPlayers.put(player, serverName);
    }

    /**
     * Removes the players entry in the delayed join list.
     *
     * @param player the player to be removed from the queue
     */
    public void cancelDelayedPlayerJoin(Player player) {
        waitingPlayers.remove(player);
    }

    /**
     * Makes sure that any players waiting in the delayed join list for
     * the given server are connected to it.
     *
     * @param serverName the server for which to handle delayed joins
     */
    private void doDelayedJoins(String serverName) {
        RegisteredServer server = getServer(serverName);

        // Guard - Server must still be registered
        if (server == null) {
            cancelDelayedJoins(serverName);
            return;
        }

        for (Map.Entry<Player, String> entry : waitingPlayers.entrySet()) {
            if (entry.getValue().equals(serverName)) {
                Player player = entry.getKey();

                waitingPlayers.remove(player);

                // Notify player
                player.sendMessage(
                    translationsManager.translateAsMiniMessage(
                        "connecting_to_server",
                        Map.of(
                            "server",
                            serverName
                        )
                    )
                );

                // Connect player to server
                player.createConnectionRequest(server).connectWithIndication();
            }
        }
    }

    /**
     * Removes delayed join list entries of players waiting for the given server.
     *
     * @param serverName the server for which to cancel delayed joins
     */
    private void cancelDelayedJoins(String serverName) {
        for (Map.Entry<Player, String> entry : waitingPlayers.entrySet()) {
            if (entry.getValue().equals(serverName)) {
                Player player = entry.getKey();

                waitingPlayers.remove(player);
            }
        }
    }

    /**
     * Adds a source to the listening sources list of the specified server
     * When the server startup script finishes (either by fail or success),
     * the source will automatically be notified about the status.
     * <p>
     * If the source was already in a listening sources list for a different
     * server, that entry will be removed.
     *
     * @param source the source to be sent notifications
     * @param serverName the name of the server whose notifications to listen to
     */
    public void setSourceNotifications(CommandSource source, String serverName) {
        listeningSources.put(source, serverName);
    }

    /**
     * Removes the sources entry in the listening sources list.
     *
     * @param source the source to be removed from the list
     */
    public void cancelSourceNotifications(CommandSource source) {
        listeningSources.remove(source);
    }

    /**
     * Sends a notification to every player listening to a specific server
     * boot-up process, telling whether the startup has succeeded or not.
     *
     * @param serverName the name of the server
     * @param success whether the startup has succeeded or not
     */
    private void notifyListeningSources(String serverName, boolean success) {
        for (Map.Entry<CommandSource, String> entry : listeningSources.entrySet()) {
            if (entry.getValue().equals(serverName)) {
                CommandSource source = entry.getKey();

                // Notify source
                if (success) {
                    source.sendMessage(
                        translationsManager.translateAsMiniMessage(
                            "server_startup.script_result.success",
                            Map.of(
                                "server",
                                serverName
                            )
                        )
                    );
                } else {
                    source.sendMessage(
                        translationsManager.translateAsMiniMessage(
                            "server_startup.script_result.unknown_error",
                            Map.of(
                                "server",
                                serverName
                            )
                        )
                    );
                }
            }
        }
    }

    /**
     * Removes sources, listening to the given server, from the listening sources list.
     *
     * @param serverName the server for which to cancel delayed joins
     */
    private void removeListeningSources(String serverName) {
        for (Map.Entry<CommandSource, String> entry : listeningSources.entrySet()) {
            if (entry.getValue().equals(serverName)) {
                CommandSource source = entry.getKey();

                listeningSources.remove(source);
            }
        }
    }
}
