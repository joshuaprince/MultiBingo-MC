package com.jtprince.bingo.plugin;

import org.jetbrains.annotations.NotNull;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class MCBConfig {
    /**
     * Determine whether debugging functions are enabled.
     */
    public static boolean getDebug() {
        return MCBingoPlugin.instance().getConfig().getBoolean("debug", false);
    }

    /**
     * Get the WebSocket URL to connect to the Bingo webserver hosting a game.
     * @param gameCode Game code that will be put in the URL.
     */
    public static URI getWebsocketUrl(String gameCode) {
        String template = MCBingoPlugin.instance().getConfig().getString("urls.websocket");
        if (template == null) {
            MCBingoPlugin.logger().severe("No websocket URL is configured!");
            return null;
        }
        try {
            return new URI(template.replace("$code", URLEncoder.encode(gameCode, StandardCharsets.UTF_8)));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a URL that a player can click to open their bingo board for a certain game.
     * @param gameCode Game code that will be put in the URL.
     * @param p The player whose board will be shown at this URL.
     */
    public static URL getGameUrl(@NotNull String gameCode, @NotNull BingoPlayer p) {
        String template = MCBingoPlugin.instance().getConfig().getString("urls.game_player");
        if (template == null) {
            MCBingoPlugin.logger().severe("No game_player URL is configured!");
            return null;
        }
        try {
            return new URL(template
                .replace("$code", URLEncoder.encode(gameCode, StandardCharsets.UTF_8))
                .replace("$player", URLEncoder.encode(p.getName(), StandardCharsets.UTF_8))
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
