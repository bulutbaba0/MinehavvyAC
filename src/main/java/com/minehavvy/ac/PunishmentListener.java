package com.minehavvy.ac;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PunishmentListener implements Listener {

    private final MineHavvyAC plugin;

    public PunishmentListener(MineHavvyAC plugin) {
        this.plugin = plugin;
    }

    // 1. HAREKETİ TAMAMEN KİLİTLE (Sadece kafasını çevirebilir)
    @EventHandler
    public void onFrozenMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isBeingPunished(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            Location locked = from.clone();
            locked.setYaw(to.getYaw());
            locked.setPitch(to.getPitch());
            event.setTo(locked);
        }
    }

    // 2. HASAR ALMAYI TAMAMEN ENGELLE (Düşme, PvP, Tükürük vb.)
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (plugin.isBeingPunished(player.getUniqueId()) || plugin.hasFallProtection(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // 3. EŞYA ATMAYI ENGELLE
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.isBeingPunished(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // 4. KAÇMAK İÇİN KOMUT KULLANMAYI ENGELLE (/spawn vb.)
    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (plugin.isBeingPunished(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§8[§6§lMineHavvy AC§8] §cCeza animasyonu sırasında komut kullanamazsınız!");
        }
    }
}
