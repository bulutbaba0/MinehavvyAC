package com.minehavvy.ac;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CombatChecks implements Listener {

    private final MineHavvyAC plugin;
    private final Map<UUID, List<Long>> clicks = new HashMap<>();

    public CombatChecks(MineHavvyAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (attacker.getGameMode() == GameMode.CREATIVE) return;

        double distance = attacker.getEyeLocation().distance(event.getEntity().getLocation());
        double maxReach = 3.45;

        if (distance > maxReach) {
            event.setCancelled(true);
            String distStr = String.format("%.2f", distance);
            plugin.flag(attacker, "Reach", "(Mesafe: " + distStr + "m)", 8, "Uzaktan Vurma (Reach)");
        }
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        clicks.putIfAbsent(uuid, new ArrayList<>());
        List<Long> playerClicks = clicks.get(uuid);
        playerClicks.add(now);

        playerClicks.removeIf(time -> now - time > 1000);

        if (playerClicks.size() > 22) {
            plugin.flag(player, "AutoClicker", "(CPS: " + playerClicks.size() + ")", 10, "Makro / AutoClicker");
        }
    }
}
