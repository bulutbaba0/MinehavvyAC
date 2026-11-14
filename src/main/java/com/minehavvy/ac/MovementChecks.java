package com.minehavvy.ac;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementChecks implements Listener {

    private final MineHavvyAC plugin;
    private final Map<UUID, Integer> airTicks = new HashMap<>();

    public MovementChecks(MineHavvyAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.isBeingPunished(player.getUniqueId())) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (player.isFlying() || player.isGliding() || player.isInsideVehicle()) return;

        UUID uuid = player.getUniqueId();
        double deltaX = event.getTo().getX() - event.getFrom().getX();
        double deltaZ = event.getTo().getZ() - event.getFrom().getZ();
        double deltaY = event.getTo().getY() - event.getFrom().getY();
        double horizontalSpeed = Math.hypot(deltaX, deltaZ);

        // 1. SPEED (HIZ HİLESİ) KONTROLÜ
        if (horizontalSpeed > 0.85 && deltaY <= 0.42) {
            event.setCancelled(true);
            String speedStr = String.format("%.2f", horizontalSpeed);
            plugin.flag(player, "Speed", "(Hız: " + speedStr + ")", 10, "Hız Hilesi (Speed)");
            return;
        }

        // 2. FLY (UÇMA HİLESİ) KONTROLÜ
        boolean onGround = player.getLocation().subtract(0, 0.2, 0).getBlock().getType() != Material.AIR;
        if (onGround) {
            airTicks.put(uuid, 0);
            return;
        }

        int ticks = airTicks.getOrDefault(uuid, 0) + 1;
        airTicks.put(uuid, ticks);

        if (ticks > 25 && deltaY >= 0.0 && player.getVelocity().getY() <= 0) {
            event.setCancelled(true);
            plugin.flag(player, "Fly", "(Havada kalma: " + ticks + " tick)", 12, "Uçma Hilesi (Fly)");
        }
    }
}
