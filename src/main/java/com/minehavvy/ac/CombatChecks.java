package com.minehavvy.ac;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CombatChecks implements Listener {

    private final MineHavvyAC plugin;
    private final Map<UUID, List<Long>> clicks = new HashMap<>();
    private final Map<UUID, Float> lastYaw = new HashMap<>();
    private final Map<UUID, Float> lastPitch = new HashMap<>();
    private final Map<UUID, Float> lastYawDelta = new HashMap<>();

    public CombatChecks(MineHavvyAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRotationTrack(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        float yaw = event.getTo().getYaw();
        float pitch = event.getTo().getPitch();

        if (lastYaw.containsKey(uuid)) {
            float diff = Math.abs(wrapAngle(yaw - lastYaw.get(uuid)));
            lastYawDelta.put(uuid, diff);
        }
        lastYaw.put(uuid, yaw);
        lastPitch.put(uuid, pitch);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (plugin.isBeingPunished(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (attacker.getGameMode() == GameMode.CREATIVE || attacker.getGameMode() == GameMode.SPECTATOR) return;

        Entity target = event.getEntity();
        Location eyeLoc = attacker.getEyeLocation();
        Vector eyeVec = eyeLoc.toVector();
        Vector lookDir = eyeLoc.getDirection().normalize();

        // 1. GERCEK HITBOX (BOUNDING BOX) + PING TELAFI REACH
        BoundingBox box = target.getBoundingBox().clone().expand(0.12);
        double closestX = clamp(eyeVec.getX(), box.getMinX(), box.getMaxX());
        double closestY = clamp(eyeVec.getY(), box.getMinY(), box.getMaxY());
        double closestZ = clamp(eyeVec.getZ(), box.getMinZ(), box.getMaxZ());

        double realDistance = eyeVec.distance(new Vector(closestX, closestY, closestZ));
        int ping = Math.min(attacker.getPing(), 250);
        double pingBonus = (ping / 1000.0) * 1.4;
        double maxReach = (attacker.isGliding() ? 3.35 : 3.18) + pingBonus;

        if (realDistance > maxReach) {
            event.setCancelled(true);
            String distStr = String.format("%.2f", realDistance);
            plugin.flag(attacker, "Reach", "(Hitbox: " + distStr + "m | Ping: " + ping + "ms)", 7, "Uzaktan Vurma (Reach)");
            return;
        }

        // 2. GORUŞ ACISI (FOV) VE ELYTRA TARGET KONTROLU
        Vector targetCenter = box.getCenter();
        Vector toTarget = targetCenter.clone().subtract(eyeVec).normalize();
        double dot = clamp(lookDir.dot(toTarget), -1.0, 1.0);
        double angleDeg = Math.toDegrees(Math.acos(dot));

        float yawSnap = lastYawDelta.getOrDefault(attacker.getUniqueId(), 0f);

        if (attacker.isGliding()) {
            // Elytra ile ucus sirasinda kilitlenme (ElytraTarget) kontrolleri
            if (angleDeg > 42.0 && realDistance > 1.6) {
                event.setCancelled(true);
                plugin.flag(attacker, "ElytraTarget", "(Acı Sapması: " + (int) angleDeg + "°)", 5, "Elytra Target Hilesi");
                return;
            }
            if (yawSnap > 68.0f && angleDeg < 18.0) {
                event.setCancelled(true);
                plugin.flag(attacker, "ElytraTarget", "(Ani Kilitlenme: " + (int) yawSnap + "°)", 5, "Elytra Target Hilesi");
                return;
            }
        } else {
            // Normal PvP KillAura FOV ve ani kafa cevirme kontrolu
            if (angleDeg > 68.0 && realDistance > 1.5) {
                event.setCancelled(true);
                plugin.flag(attacker, "KillAura", "(Görüş Dışı: " + (int) angleDeg + "°)", 6, "KillAura (Açı Dışı Vuruş)");
                return;
            }
            if (yawSnap > 95.0f && angleDeg < 12.0 && realDistance > 1.8) {
                event.setCancelled(true);
                plugin.flag(attacker, "KillAura", "(Snap Dönüş: " + (int) yawSnap + "°)", 6, "KillAura (Ani Kilitlenme)");
                return;
            }
        }

        // 3. DUVAR ARKASI VURUS (WALLHIT) KONTROLU
        if (target instanceof LivingEntity && realDistance > 1.8) {
            RayTraceResult blockTrace = attacker.getWorld().rayTraceBlocks(
                    eyeLoc,
                    toTarget,
                    realDistance - 0.3,
                    FluidCollisionMode.NEVER,
                    true
            );
            if (blockTrace != null && blockTrace.getHitBlock() != null && blockTrace.getHitBlock().getType().isOccluding()) {
                event.setCancelled(true);
                plugin.flag(attacker, "WallHit", "(Blok Arkası Vuruş)", 5, "Duvar Arkası Vurma (KillAura)");
            }
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

        if (playerClicks.size() > 21) {
            plugin.flag(player, "AutoClicker", "(CPS: " + playerClicks.size() + ")", 8, "Makro / AutoClicker");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clicks.remove(uuid);
        lastYaw.remove(uuid);
        lastPitch.remove(uuid);
        lastYawDelta.remove(uuid);
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private float wrapAngle(float angle) {
        angle %= 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }
}
