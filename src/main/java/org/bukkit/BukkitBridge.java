package org.bukkit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import java.lang.annotation.*;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Logger;

public class Bukkit {
    public static Server getServer() { return null; }
    public static Logger getLogger() { return null; }
    public static Collection<? extends Player> getOnlinePlayers() { return null; }
    public static BukkitScheduler getScheduler() { return null; }
    public static CommandSender getConsoleSender() { return null; }
    public static boolean dispatchCommand(CommandSender sender, String commandLine) { return false; }
}
interface Server { PluginManager getPluginManager(); }
enum GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }
enum Material { AIR }
class Location {
    public double getY() { return 0; }
    public double distance(Location o) { return 0; }
    public Location subtract(double x, double y, double z) { return this; }
    public Block getBlock() { return null; }
}
interface Block { Material getType(); }
