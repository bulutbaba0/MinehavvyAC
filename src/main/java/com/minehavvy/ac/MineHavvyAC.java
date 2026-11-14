package com.minehavvy.ac;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Llama;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MineHavvyAC extends JavaPlugin implements CommandExecutor {

    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Set<UUID> punishing = new HashSet<>();
    private final Set<UUID> alertsDisabled = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new CombatChecks(this), this);
        getServer().getPluginManager().registerEvents(new MovementChecks(this), this);
        if (getCommand("mhac") != null) {
            getCommand("mhac").setExecutor(this);
        }

        // Her 15 saniyede bir oyuncuların uyarı puanını (VL) 1 azalt (Yanlış ban önleyici)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : new ArrayList<>(violations.keySet())) {
                    int vl = violations.get(uuid) - 1;
                    if (vl <= 0) {
                        violations.remove(uuid);
                    } else {
                        violations.put(uuid, vl);
                    }
                }
            }
        }.runTaskTimer(this, 300L, 300L);

        getLogger().info("MineHavvyAC v2.0 (Pro) basariyla aktif edildi!");
    }

    public boolean isBeingPunished(UUID uuid) {
        return punishing.contains(uuid);
    }

    public void flag(Player player, String checkName, String details, int maxVl, String banReason) {
        if (player.hasPermission("minehavvy.ac.bypass")) return;
        if (punishing.contains(player.getUniqueId())) return;

        UUID uuid = player.getUniqueId();
        int currentVl = violations.getOrDefault(uuid, 0) + 1;
        violations.put(uuid, currentVl);

        String alert = "§8[§6§lMineHavvy AC§8] §e" + player.getName() +
                " §c" + checkName + " §7şüphesi §8(§c" + currentVl + "/" + maxVl + " VL§8) §7" + details;

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("minehavvy.ac.alerts") && !alertsDisabled.contains(staff.getUniqueId())) {
                staff.sendMessage(alert);
            }
        }

        if (currentVl >= maxVl) {
            violations.remove(uuid);
            startLlamaPunishment(player, banReason, true);
        }
    }

    public void startLlamaPunishment(Player player, String banReason, boolean executeBan) {
        UUID uuid = player.getUniqueId();
        if (punishing.contains(uuid)) return;
        punishing.add(uuid);

        if (executeBan) {
            Bukkit.broadcastMessage("§8§m----------------------------------------");
            Bukkit.broadcastMessage("§6§lMineHavvy AC §8» §e" + player.getName() + " §chileden yakalandı!");
            Bukkit.broadcastMessage("§7Sebep: §f" + banReason);
            Bukkit.broadcastMessage("§8§m----------------------------------------");
        }

        Location baseLoc = player.getLocation().clone().add(0, 4.0, 0);
        player.teleport(baseLoc);
        player.setAllowFlight(true);
        player.setFlying(true);

        List<Llama> llamas = new ArrayList<>();
        double[][] offsets = {{2.5, 0}, {-2.5, 0}, {0, 2.5}, {0, -2.5}};
        for (double[] off : offsets) {
            Location llamaLoc = baseLoc.clone().add(off, 0, off);
            Llama llama = (Llama) player.getWorld().spawnEntity(llamaLoc, EntityType.LLAMA);
            llama.setAI(false);
            llama.setInvulnerable(true);
            llama.setTarget(player);
            llamas.add(llama);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 80) {
                    for (Llama l : llamas) {
                        l.remove();
                    }
                    punishing.remove(uuid);
                    if (player.isOnline()) {
                        player.setFlying(false);
                        player.setAllowFlight(false);
                        if (executeBan) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                                    "ban " + player.getName() + " 14d [MineHavvyAC] " + banReason);
                        } else {
                            player.sendMessage("§8[§6§lMineHavvy AC§8] §aTest animasyonu tamamlandı (Ban atılmadı).");
                        }
                    }
                    cancel();
                    return;
                }

                player.setVelocity(new Vector(0, 0, 0));

                if (ticks % 20 == 0) {
                    player.sendTitle("§c§lHİLE AÇTIĞIN İÇİN", "§e§lMUTLU MUSUN?", 0, 25, 5);
                    player.playSound(player.getLocation(), Sound.ENTITY_LLAMA_SPIT, 1.0f, 1.0f);
                }

                for (Llama llama : llamas) {
                    Location lLoc = llama.getLocation();
                    Vector dir = player.getLocation().toVector().subtract(lLoc.toVector()).normalize();
                    lLoc.setDirection(dir);
                    llama.teleport(lLoc);

                    if (ticks % 10 == 0) {
                        llama.launchProjectile(LlamaSpit.class, dir.multiply(1.2));
                    }
                }

                ticks += 5;
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("minehavvy.ac.alerts")) {
            sender.sendMessage("§8[§6§lMineHavvy AC§8] §cBu komutu kullanmak için yetkiniz yok!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§8§m----------------------------------------");
            sender.sendMessage("§6§lMineHavvy AC v2.0 §8- §7Yönetim Komutları");
            sender.sendMessage("§e/mhac alerts §8- §7Hile uyarılarını açar/kapatır.");
            sender.sendMessage("§e/mhac test <oyuncu> §8- §7Lama cezasını bansız test eder.");
            sender.sendMessage("§e/mhac temizle <oyuncu> §8- §7Oyuncunun uyarı puanını sıfırlar.");
            sender.sendMessage("§8§m----------------------------------------");
            return true;
        }

        if (args[0].equalsIgnoreCase("alerts") && sender instanceof Player p) {
            if (alertsDisabled.contains(p.getUniqueId())) {
                alertsDisabled.remove(p.getUniqueId());
                p.sendMessage("§8[§6§lMineHavvy AC§8] §aHile uyarıları açıldı.");
            } else {
                alertsDisabled.add(p.getUniqueId());
                p.sendMessage("§8[§6§lMineHavvy AC§8] §cHile uyarıları kapatıldı.");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("test") && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§8[§6§lMineHavvy AC§8] §cOyuncu bulunamadı!");
                return true;
            }
            sender.sendMessage("§8[§6§lMineHavvy AC§8] §e" + target.getName() + " §aüzerinde test başlatıldı!");
            startLlamaPunishment(target, "Test Cezası", false);
            return true;
        }

        if (args[0].equalsIgnoreCase("temizle") && args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§8[§6§lMineHavvy AC§8] §cOyuncu bulunamadı!");
                return true;
            }
            violations.remove(target.getUniqueId());
            sender.sendMessage("§8[§6§lMineHavvy AC§8] §e" + target.getName() + " §aadlı oyuncunun uyarı puanları temizlendi.");
            return true;
        }

        return true;
    }
}
