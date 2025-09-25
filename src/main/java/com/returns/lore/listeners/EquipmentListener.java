package com.returns.lore.listeners;

import com.returns.lore.LorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

public class EquipmentListener implements Listener {

    private final LorePlugin plugin;

    public EquipmentListener(LorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getStatManager().updatePlayerStats(player);
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 플레이어 나갈 때 데이터 정리는 StatManager에서 처리
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getStatManager().updatePlayerStats(player);
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getStatManager().updatePlayerStats(player);
            }
        }.runTaskLater(plugin, 3L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getStatManager().updatePlayerStats(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getType() == InventoryType.PLAYER ||
            event.getInventory().getType() == InventoryType.CRAFTING) {

            if (isArmorSlot(event.getSlot()) || event.getSlot() == player.getInventory().getHeldItemSlot() ||
                (event.getSlot() >= 0 && event.getSlot() <= 8)) {

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        plugin.getStatManager().updatePlayerStats(player);
                    }
                }.runTaskLater(plugin, 2L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getStatManager().updatePlayerStats(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    private boolean isArmorSlot(int slot) {
        return slot >= 36 && slot <= 39;
    }
}