package com.minehavvy.ac;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementChecks implements Listener {

    private final MineHavvyAC plugin;
    private final Map<UUID, Integer> airTicks = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final Map<UUID, Long> lastFireworkTime = new HashMap<>();

    public MovementChecks(MineHavvyAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            lastDamageTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onFirework(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isGliding()) return;
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.FIREWORK_ROCKET) {
            lastFireworkTime.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (plugin.isBeingPunished(uuid)) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        if (player.isFlying() || player.isInsideVehicle()) return;

        double deltaX = event.getTo().getX() - event.getFrom().getX();
        double deltaZ = event.getTo().getZ() - event.getFrom().getZ();
        double deltaY = event.getTo().getY() - event.getFrom().getY();
        double horizontalSpeed = Math.hypot(deltaX, deltaZ);

        long now = System.currentTimeMillis();
        boolean recentlyDamaged = (now - lastDamageTime.getOrDefault(uuid, 0L)) < 1200;
        boolean recentlyBoosted = (now - lastFireworkTime.getOrDefault(uuid, 0L)) < 2500;

        // 1. ELYTRA FLY / ELYTRA SPEED KONTROLU
        if (player.isGliding()) {
            double maxElytraSpeed = recentlyBoosted ? 2.15 : 1.55;
            if (horizontalSpeed > maxElytraSpeed && !recentlyDamaged) {
                event.setTo(event.getFrom());
                String spd = String.format("%.2f", horizontalSpeed);
                plugin.flag(player, "ElytraSpeed", "(Hız: " + spd + ")", 7, "Elytra Hız / Uçuş Hilesi");
                return;
            }
            // Havai fisek basmadan duz veya yukari dogru uzun sureli tırmanma (ElytraFly)
            if (!recentlyBoosted && !recentlyDamaged && deltaY > 0.18 && horizontalSpeed < 0.65) {
                event.setTo(event.getFrom());
                plugin.flag(player, "ElytraFly", "(Havada Yükselme)", 7, "Elytra Uçuş Hilesi");
            }
            return;
        }

        // 2. NORMAL SPEED (HIZ HILESI) KONTROLU
        Material below = player.getLocation().clone().subtract(0, 0.5, 0).getBlock().getType();
        boolean onIce = below.name().contains("ICE");
        double maxGroundSpeed = (recentlyDamaged ? 1.35 : (onIce ? 1.15 : 0.78));

        if (horizontalSpeed > maxGroundSpeed && deltaY <= 0.42 && !player.isRiptiding()) {
            event.setTo(event.getFrom());
            String speedStr = String.format("%.2f", horizontalSpeed);
            plugin.flag(player, "Speed", "(Hız: " + speedStr + ")", 9, "Hız Hilesi (Speed)");
            return;
        }

        // 3. FLY (UCMA HILESI) KONTROLU
        boolean onGround = player.getLocation().clone().subtract(0, 0.25, 0).getBlock().getType() != Material.AIR
                || player.isInWater()
                || player.isClimbing();

        if (onGround || recentlyDamaged) {
            airTicks.put(uuid, 0);
            return;
        }

        int ticks = airTicks.getOrDefault(uuid, 0) + 1;
        airTicks.put(uuid, ticks);

        if (ticks > 28 && deltaY >= -0.02 && player.getVelocity().getY() <= 0) {
            event.setTo(event.getFrom());
            plugin.flag(player, "Fly", "(Havada kalma: " + ticks + " tick)", 10, "Uçma Hilesi (Fly)");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        airTicks.remove(uuid);
        lastDamageTime.remove(uuid);
        lastFireworkTime.remove(uuid);
    }
}
