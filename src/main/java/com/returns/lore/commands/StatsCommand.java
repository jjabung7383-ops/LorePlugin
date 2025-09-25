package com.returns.lore.commands;

import com.returns.lore.LorePlugin;
import com.returns.lore.utils.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final LorePlugin plugin;

    public StatsCommand(LorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!player.hasPermission("lore.use")) {
            player.sendMessage(Component.text(
                plugin.getConfig().getString("messages.no-permission", "&c권한이 없습니다.")
                    .replace("&", "§")
            ).color(NamedTextColor.RED));
            return true;
        }

        // 먼저 능력치를 강제로 업데이트
        plugin.getStatManager().updatePlayerStats(player);

        Map<StatType, Double> stats = plugin.getStatManager().getPlayerStats(player.getUniqueId());

        boolean debug = plugin.getConfig().getBoolean("debug", false);
        if (debug) {
            plugin.getLogger().info("플레이어 " + player.getName() + "의 /스텟 명령어 실행:");
            plugin.getLogger().info("  저장된 능력치 데이터: " + stats.size() + "개");
            for (StatType type : StatType.values()) {
                double value = stats.getOrDefault(type, 0.0);
                if (value > 0) {
                    plugin.getLogger().info("    " + type.getDisplayName() + ": " + value);
                }
            }
        }

        player.sendMessage(Component.text("").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("========== 내 능력치 ==========").color(NamedTextColor.GOLD));

        boolean hasAnyStats = false;
        for (StatType statType : StatType.values()) {
            double value = stats.getOrDefault(statType, 0.0);
            if (value > 0) {
                hasAnyStats = true;
                String formattedValue;
                if (value == (long) value) {
                    formattedValue = String.valueOf((long) value);
                } else {
                    formattedValue = String.format("%.1f", value);
                }

                player.sendMessage(Component.text(
                    statType.getDisplayName() + ": +" + formattedValue + getStatUnit(statType)
                ).color(NamedTextColor.AQUA));
            }
        }

        if (!hasAnyStats) {
            player.sendMessage(Component.text("적용된 능력치가 없습니다.").color(NamedTextColor.GRAY));
        }

        player.sendMessage(Component.text("================================").color(NamedTextColor.GOLD));

        return true;
    }

    private String getStatUnit(StatType statType) {
        return switch (statType) {
            case 치명타확률, 흡혈률, 회피율 -> "%";
            case 이동속도 -> "%";
            default -> "";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}