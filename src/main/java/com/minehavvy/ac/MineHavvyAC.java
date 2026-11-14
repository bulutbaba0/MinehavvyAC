package com.minehavvy.ac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MineHavvyAC extends JavaPlugin {

    private final Map<UUID, Integer> violations = new HashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new CombatChecks(this), this);
        getServer().getPluginManager().registerEvents(new MovementChecks(this), this);
        getLogger().info("MineHavvyAC başarıyla aktif edildi!");
    }

    public void flag(Player player, String checkName, String details, int maxVl, String banReason) {
        if (player.hasPermission("minehavvy.ac.bypass")) return;

        UUID uuid = player.getUniqueId();
        int currentVl = violations.getOrDefault(uuid, 0) + 1;
        violations.put(uuid, currentVl);

        Component alert = mm.deserialize(
            "<dark_gray>[<gold><bold>MineHavvy AC</bold><dark_gray>] <yellow>" + player.getName() +
            " <red>" + checkName + " <gray>şüphesi <dark_gray>(<red>" + currentVl + "/" + maxVl + " VL<dark_gray>) <gray>" + details
        );

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("minehavvy.ac.alerts")) {
                staff.sendMessage(alert);
            }
        }

        if (currentVl >= maxVl) {
            violations.remove(uuid);
            Bukkit.getScheduler().runTask(this, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "ban " + player.getName() + " 14d [MineHavvyAC] " + banReason)
            );
        }
    }
}
