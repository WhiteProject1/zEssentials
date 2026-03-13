package fr.maxlego08.essentials.module.modules.trade;

import org.bukkit.entity.Player;

import java.util.UUID;

public class TradeRequest {

    private final Player player1;
    private final Player player2;
    private final long timestamp;

    public TradeRequest(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.timestamp = System.currentTimeMillis();
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isExpired(long timeoutMillis) {
        return System.currentTimeMillis() - timestamp > timeoutMillis;
    }
}
