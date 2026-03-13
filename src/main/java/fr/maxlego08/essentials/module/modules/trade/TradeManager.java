package fr.maxlego08.essentials.module.modules.trade;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.module.modules.trade.inventory.TradeInventoryHolder;
import fr.maxlego08.essentials.module.modules.trade.model.TradeSession;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager {

    private final ZEssentialsPlugin plugin;
    private final TradeModule tradeModule;
    private final Map<UUID, TradeRequest> activeTrades = new HashMap<>();
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();

    public TradeManager(ZEssentialsPlugin plugin, TradeModule tradeModule) {
        this.plugin = plugin;
        this.tradeModule = tradeModule;
    }

    public void sendRequest(Player sender, Player target) {
        if (sender.equals(target)) {
            tradeModule.sendMessage(sender, "cannot-trade-self");
            return;
        }

        UUID senderUUID = sender.getUniqueId();
        UUID targetUUID = target.getUniqueId();

        if (pendingRequests.containsKey(senderUUID)) {
            tradeModule.sendMessage(sender, "already-sent-request");
            return;
        }

        pendingRequests.put(targetUUID, senderUUID);
        tradeModule.sendMessage(sender, "request-sent", "%player%", target.getName());
        tradeModule.sendMessage(target, "request-received", "%player%", sender.getName());
    }

    public void acceptRequest(Player requester, Player accepter) {
        UUID accepterUUID = accepter.getUniqueId();
        UUID requesterUUID = requester.getUniqueId();

        pendingRequests.remove(accepterUUID);

        TradeSession session = new TradeSession(requester, accepter);
        TradeRequest request = new TradeRequest(requester, accepter);
        activeTrades.put(requesterUUID, request);
        activeTrades.put(accepterUUID, request);

        new TradeInventoryHolder(requester, session, tradeModule).open();
        new TradeInventoryHolder(accepter, session, tradeModule).open();
    }

    public void denyRequest(Player requester, Player denier) {
        UUID denierUUID = denier.getUniqueId();
        pendingRequests.remove(denierUUID);
        tradeModule.sendMessage(requester, "request-denied", "%player%", denier.getName());
        tradeModule.sendMessage(denier, "request-deny-success", "%player%", requester.getName());
    }

    public Map<UUID, UUID> getRequests() {
        return pendingRequests;
    }

    public void cancelAllTrades() {
        for (TradeRequest request : activeTrades.values()) {
            if (request != null) {
                Player player1 = request.getPlayer1();
                Player player2 = request.getPlayer2();
                if (player1 != null && player1.isOnline()) {
                    player1.closeInventory();
                }
                if (player2 != null && player2.isOnline()) {
                    player2.closeInventory();
                }
            }
        }
        activeTrades.clear();
        pendingRequests.clear();
    }

    public Map<UUID, TradeRequest> getActiveTrades() {
        return activeTrades;
    }
}
