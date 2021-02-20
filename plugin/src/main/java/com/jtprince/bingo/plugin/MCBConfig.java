package com.jtprince.bingo.plugin;

import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

import java.net.*;
import java.util.logging.Level;

public class MCBConfig {
    /**
     * Determine whether debugging functions are enabled.
     */
    public static boolean getDebug() {
        return MCBingoPlugin.instance().getConfig().getBoolean("debug", false);
    }

    /**
     * Get the URL to use to generate a board with settings.
     */
    public static URI getBoardCreateUrl() {
        String template = MCBingoPlugin.instance().getConfig().getString("web_url");
        if (template == null) {
            MCBingoPlugin.logger().severe("No web_url is configured!");
            return null;
        }

        try {
            URIBuilder builder = new URIBuilder(template);

            builder.setPathSegments("rest", "generate_board");

            return builder.build();
        } catch (URISyntaxException e) {
            MCBingoPlugin.logger().log(Level.SEVERE, "Misconfigured web_url", e);
            return null;
        }
    }

    /**
     * Get the WebSocket URL to connect to the Bingo webserver hosting a game.
     * @param gameCode Game code that will be put in the URL, or null if it is improperly configured.
     */
    public static URI getWebsocketUrl(String gameCode) {
        String template = MCBingoPlugin.instance().getConfig().getString("web_url");
        if (template == null) {
            MCBingoPlugin.logger().severe("No web_url is configured!");
            return null;
        }

        try {
            URIBuilder builder = new URIBuilder(template);

            if (builder.getScheme().equalsIgnoreCase("https")) {
                builder.setScheme("wss");
            } else if (builder.getScheme().equalsIgnoreCase("http")) {
                builder.setScheme("ws");
            } else {
                throw new URISyntaxException(template, "Scheme must be http or https");
            }

            builder.setPathSegments("ws", "board-plugin", gameCode);

            return builder.build();
        } catch (URISyntaxException e) {
            MCBingoPlugin.logger().log(Level.SEVERE, "Misconfigured web_url", e);
            return null;
        }
    }

    /**
     * Get a URL that a player can click to open their bingo board for a certain game.
     * @param gameCode Game code that will be put in the URL.
     * @param p The player whose board will be shown at this URL, or null if it is improperly
     *          configured.
     */
    public static URL getGameUrl(@NotNull String gameCode, @NotNull BingoPlayer p) {
        String template = MCBingoPlugin.instance().getConfig().getString("web_url");
        if (template == null) {
            MCBingoPlugin.logger().severe("No web_url is configured!");
            return null;
        }

        try {
            URIBuilder builder = new URIBuilder(template);
            builder.setPathSegments("game", gameCode);
            builder.setParameter("name", p.getName());

            return builder.build().toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            MCBingoPlugin.logger().log(Level.SEVERE, "Misconfigured web_url", e);
            return null;
        }
    }

    /**
     * Returns whether all bingo worlds should be saved to disk when a game ends.
     */
    public static boolean getSaveWorlds() {
        return MCBingoPlugin.instance().getConfig().getBoolean("save_worlds", true);
    }
}
