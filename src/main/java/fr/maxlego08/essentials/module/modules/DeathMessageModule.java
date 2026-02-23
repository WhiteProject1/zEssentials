package fr.maxlego08.essentials.module.modules;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.commands.Permission;
import fr.maxlego08.essentials.api.messages.Message;
import fr.maxlego08.essentials.api.modules.death.DeathMessageType;
import fr.maxlego08.essentials.api.modules.death.MythicMobsHook;
import fr.maxlego08.essentials.module.ZModule;
import fr.maxlego08.essentials.zutils.utils.paper.PaperComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DeathMessageModule extends ZModule {

    private boolean allowSilentDeath;
    private DeathMessageType deathMessageType = DeathMessageType.DEFAULT;
    private MythicMobsHook mythicMobsHook;

    private final Map<String, List<String>> customMessages = new HashMap<>();

    public DeathMessageModule(ZEssentialsPlugin plugin) {
        super(plugin, "death_message");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();

        YamlConfiguration configuration = getConfiguration();

        this.customMessages.clear();

        ConfigurationSection section = configuration.getConfigurationSection("custom-messages");
        if (section != null) {
            for (String cause : section.getKeys(false)) {
                List<String> messages = section.getStringList(cause);
                if (!messages.isEmpty()) {
                    this.customMessages.put(cause.toUpperCase(), messages);
                }
            }
        }
    }

    public void setMythicMobsHook(MythicMobsHook mythicMobsHook) {
        this.mythicMobsHook = mythicMobsHook;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent event) {

        if (!isEnable()) return;

        Player player = event.getEntity();

        if (this.allowSilentDeath && hasPermission(player, Permission.ESSENTIALS_SILENT_DEATH)) {
            event.deathMessage(Component.empty());
            return;
        }

        if (this.deathMessageType == DeathMessageType.DISABLE) {
            event.deathMessage(Component.empty());
            return;
        }

        if (this.deathMessageType == DeathMessageType.DEFAULT) {
            return;
        }

        String deathMessage = buildDeathMessage(player);
        if (deathMessage == null || deathMessage.isEmpty()) {
            return;
        }

        PaperComponent paperComponent = (PaperComponent) this.componentMessage;
        var papiMessage = papi(deathMessage, player);
        event.deathMessage(paperComponent.getComponent(papiMessage));
    }

    private String buildDeathMessage(Player player) {
        EntityDamageEvent lastDamage = player.getLastDamageCause();

        if (lastDamage == null) {
            return getCustomOrDefaultMessage("GENERIC", player, null, null, null);
        }

        EntityDamageEvent.DamageCause cause = lastDamage.getCause();

        if (lastDamage instanceof EntityDamageByEntityEvent entityDamageEvent) {
            Entity damager = entityDamageEvent.getDamager();

            if (damager instanceof Player killer) {
                return getPlayerKillMessage(player, killer);
            }

            if (this.mythicMobsHook != null && this.mythicMobsHook.isMythicMob(damager)) {
                Optional<String> mobName = this.mythicMobsHook.getMythicMobName(damager);
                return getCustomOrDefaultMessage("MYTHIC_MOB", player, null, mobName.orElse("Unknown"), cause.name());
            }

            if (damager instanceof LivingEntity livingEntity) {
                String mobName = getMobName(livingEntity);
                return getCustomOrDefaultMessage("MOB", player, null, mobName, cause.name());
            }
        }

        String customMessage = getCustomMessage(cause.name());
        if (customMessage != null) {
            return formatMessage(customMessage, player, null, null, cause.name());
        }

        return getCustomOrDefaultMessage("GENERIC", player, null, null, cause.name());
    }

    private String getPlayerKillMessage(Player victim, Player killer) {
        ItemStack weapon = killer.getInventory().getItemInMainHand();
        String weaponName = weapon.getType().isAir() ? "fists" : getItemName(weapon);
        return getCustomOrDefaultMessage("PLAYER", victim, killer.getName(), weaponName, "PLAYER");
    }

    private String getCustomOrDefaultMessage(String type, Player player, String killer, String mob, String cause) {
        String customMessage = getCustomMessage(type);
        if (customMessage != null) {
            return formatMessage(customMessage, player, killer, mob, cause);
        }

        Message defaultMessage = switch (type) {
            case "PLAYER" -> Message.DEATH_MESSAGE_PLAYER;
            case "MOB" -> Message.DEATH_MESSAGE_MOB;
            case "MYTHIC_MOB" -> Message.DEATH_MESSAGE_MYTHIC_MOB;
            default -> Message.DEATH_MESSAGE_GENERIC;
        };

        String message = getMessage(defaultMessage);
        return formatMessage(message, player, killer, mob, cause);
    }

    private String getCustomMessage(String cause) {
        List<String> messages = this.customMessages.get(cause.toUpperCase());
        if (messages != null && !messages.isEmpty()) {
            return messages.get((int) (Math.random() * messages.size()));
        }
        return null;
    }

    private String formatMessage(String message, Player player, String killer, String mob, String cause) {
        message = message.replace("%player%", player.getName());
        message = message.replace("%displayName%", player.getDisplayName());
        if (killer != null) {
            message = message.replace("%killer%", killer);
        }
        if (mob != null) {
            message = message.replace("%mob%", mob);
        }
        if (cause != null) {
            message = message.replace("%cause%", formatCause(cause));
        }
        return message;
    }

    private String formatCause(String cause) {
        return cause.toLowerCase().replace("_", " ");
    }

    private String getMobName(LivingEntity entity) {
        String customName = entity.getCustomName();
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        return name(entity.getType().name());
    }

    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return name(item.getType().name());
    }
}
