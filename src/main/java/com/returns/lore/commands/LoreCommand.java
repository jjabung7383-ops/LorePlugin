package com.returns.lore.commands;

import com.returns.lore.LorePlugin;
import com.returns.lore.utils.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoreCommand implements CommandExecutor, TabCompleter {

    private final LorePlugin plugin;

    public LoreCommand(LorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("리로드")) {
            if (!player.hasPermission("lore.admin")) {
                player.sendMessage(Component.text(
                    plugin.getConfig().getString("messages.no-permission", "&c권한이 없습니다.")
                        .replace("&", "§")
                ).color(NamedTextColor.RED));
                return true;
            }

            plugin.reloadConfig();
            player.sendMessage(Component.text(
                plugin.getConfig().getString("messages.config-reloaded", "&a설정이 다시 로드되었습니다.")
                    .replace("&", "§")
            ).color(NamedTextColor.GREEN));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(Component.text("사용법: /로어 <능력치> <값> 또는 /로어 리로드").color(NamedTextColor.RED));
            player.sendMessage(Component.text("사용 가능한 능력치: " + StatType.getValidStats()).color(NamedTextColor.YELLOW));
            return true;
        }

        if (!player.hasPermission("lore.use")) {
            player.sendMessage(Component.text(
                plugin.getConfig().getString("messages.no-permission", "&c권한이 없습니다.")
                    .replace("&", "§")
            ).color(NamedTextColor.RED));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(Component.text(
                plugin.getConfig().getString("messages.no-item", "&c손에 아이템을 들고 사용해주세요.")
                    .replace("&", "§")
            ).color(NamedTextColor.RED));
            return true;
        }

        if (!isValidItem(item)) {
            player.sendMessage(Component.text(
                plugin.getConfig().getString("messages.invalid-item", "&c해당 아이템에는 로어를 적용할 수 없습니다.")
                    .replace("&", "§")
            ).color(NamedTextColor.RED));
            return true;
        }

        StatType statType = StatType.fromString(args[0]);
        if (statType == null) {
            player.sendMessage(Component.text(
                plugin.getConfig().getString("messages.invalid-stat",
                    "&c잘못된 능력치입니다. 사용 가능한 능력치: " + StatType.getValidStats())
                    .replace("&", "§")
            ).color(NamedTextColor.RED));
            return true;
        }

        double value;
        try {
            value = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("값은 숫자로 입력해주세요.").color(NamedTextColor.RED));
            return true;
        }

        if (value <= 0) {
            player.sendMessage(Component.text("값은 0보다 큰 수여야 합니다.").color(NamedTextColor.RED));
            return true;
        }

        plugin.getStatManager().addStatToItem(item, statType, value);
        plugin.getStatManager().updatePlayerStats(player);

        player.sendMessage(Component.text(
            plugin.getConfig().getString("messages.stat-added", "&a능력치가 추가되었습니다: &e%stat% +%value%")
                .replace("&", "§")
                .replace("%stat%", statType.getDisplayName())
                .replace("%value%", String.valueOf(value))
        ).color(NamedTextColor.GREEN));

        return true;
    }

    private boolean isValidItem(ItemStack item) {
        if (item == null) return false;

        String itemName = item.getType().name();
        return plugin.getConfig().getStringList("applicable-items.weapons").contains(itemName) ||
               plugin.getConfig().getStringList("applicable-items.armor").contains(itemName);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> options = new ArrayList<>();

            for (StatType statType : StatType.values()) {
                options.add(statType.getDisplayName());
            }
            options.add("리로드");

            String input = args[0].toLowerCase();
            for (String option : options) {
                if (option.toLowerCase().startsWith(input)) {
                    completions.add(option);
                }
            }
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("리로드")) {
            completions.addAll(Arrays.asList("1", "5", "10", "20", "50", "100"));
        }

        return completions;
    }
}