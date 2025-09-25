package com.returns.lore.managers;

import com.returns.lore.LorePlugin;
import com.returns.lore.utils.StatType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatManager {

    private final LorePlugin plugin;
    private final Map<UUID, Map<StatType, Double>> playerStats = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitRunnable> regenTasks = new ConcurrentHashMap<>();

    // 성능 최적화를 위한 캐시
    private final Set<Material> validWeapons = new HashSet<>();
    private final Set<Material> validArmor = new HashSet<>();
    private boolean cacheInitialized = false;

    public StatManager(LorePlugin plugin) {
        this.plugin = plugin;
        initializeCache();
        startRegenTask();
    }

    private void initializeCache() {
        // 유효한 무기/방어구 캐시 초기화
        validWeapons.clear();
        validArmor.clear();

        plugin.getConfig().getStringList("applicable-items.weapons")
            .forEach(name -> {
                try {
                    validWeapons.add(Material.valueOf(name));
                } catch (IllegalArgumentException ignored) {}
            });

        plugin.getConfig().getStringList("applicable-items.armor")
            .forEach(name -> {
                try {
                    validArmor.add(Material.valueOf(name));
                } catch (IllegalArgumentException ignored) {}
            });

        cacheInitialized = true;
    }

    public void addStatToItem(ItemStack item, StatType statType, double value) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // 기존 동일한 능력치 제거 (최적화된 버전)
        removeExistingStat(lore, statType);

        // 새 능력치 추가
        String loreFormat = getLoreFormat(statType);
        String formattedLore = loreFormat.replace("%value%", String.valueOf(value));

        lore.add(Component.text(formattedLore.replace("&", "§")));

        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private void removeExistingStat(List<Component> lore, StatType statType) {
        lore.removeIf(line -> {
            String text = PlainTextComponentSerializer.plainText().serialize(line);
            String cleanText = text.replaceAll("§[0-9a-fk-or]", "");
            return cleanText.contains(statType.getDisplayName());
        });
    }

    public void updatePlayerStats(Player player) {
        Map<StatType, Double> totalStats = calculatePlayerStats(player);
        playerStats.put(player.getUniqueId(), totalStats);
        applyStatsToPlayer(player, totalStats);

        // 디버그 로깅
        if (plugin.getConfig().getBoolean("debug", false)) {
            logPlayerStats(player, totalStats);
        }
    }

    private Map<StatType, Double> calculatePlayerStats(Player player) {
        Map<StatType, Double> stats = new EnumMap<>(StatType.class);

        // 초기값 설정 (Stream 대신 for-each 사용으로 성능 향상)
        for (StatType type : StatType.values()) {
            stats.put(type, 0.0);
        }

        // 메인핸드 아이템 체크 (캐시된 Set 사용)
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (isValidWeapon(mainHand)) {
            addItemStats(stats, mainHand);
        }

        // 방어구 체크 (캐시된 Set 사용)
        ItemStack[] armorContents = player.getInventory().getArmorContents();
        for (ItemStack armorPiece : armorContents) {
            if (isValidArmor(armorPiece)) {
                addItemStats(stats, armorPiece);
            }
        }

        // 재생력 처리
        handleRegeneration(player, stats.get(StatType.재생력));

        return stats;
    }

    private void addItemStats(Map<StatType, Double> stats, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        List<Component> lore = item.getItemMeta().lore();
        if (lore == null || lore.isEmpty()) return;

        for (Component line : lore) {
            String text = PlainTextComponentSerializer.plainText().serialize(line);
            String cleanText = text.replaceAll("§[0-9a-fk-or]", "");

            // 각 능력치 이름을 찾고 숫자를 추출
            for (StatType type : StatType.values()) {
                if (cleanText.contains(type.getDisplayName())) {
                    try {
                        // + 이후의 숫자 추출 (정규식 사용)
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\+(\\d+(?:\\.\\d+)?)");
                        java.util.regex.Matcher matcher = pattern.matcher(cleanText);

                        if (matcher.find()) {
                            double value = Double.parseDouble(matcher.group(1));
                            double multiplier = plugin.getConfig().getDouble("stat-multipliers." + type.getDisplayName(), 1.0);
                            stats.put(type, stats.get(type) + (value * multiplier));
                        }
                    } catch (NumberFormatException ignored) {
                        // 무시
                    }
                    break; // 매칭된 능력치 찾았으니 다음 라인으로
                }
            }
        }
    }

    private void applyStatsToPlayer(Player player, Map<StatType, Double> stats) {
        clearPlayerModifiers(player);

        // 속성 적용 (null 체크 포함)
        applyAttribute(player, Attribute.GENERIC_MAX_HEALTH, "lore_health", stats.get(StatType.생명력));
        applyAttribute(player, Attribute.GENERIC_ATTACK_DAMAGE, "lore_attack", stats.get(StatType.공격력));
        applyAttribute(player, Attribute.GENERIC_ARMOR, "lore_armor", stats.get(StatType.방어력));
        applyAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, "lore_speed", stats.get(StatType.이동속도) * 0.01);
    }

    private void applyAttribute(Player player, Attribute attribute, String modifierName, double value) {
        if (value <= 0) return;

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            UUID modifierId = UUID.nameUUIDFromBytes(modifierName.getBytes());

            
            Collection<AttributeModifier> modifiers = new ArrayList<>(instance.getModifiers());
            for (AttributeModifier mod : modifiers) {
                if (mod.getUniqueId().equals(modifierId)) {
                    instance.removeModifier(mod);
                }
            }

            
            boolean hasModifier = false;
            for (AttributeModifier mod : instance.getModifiers()) {
                if (mod.getUniqueId().equals(modifierId)) {
                    hasModifier = true;
                    break;
                }
            }

            if (!hasModifier) {
                AttributeModifier modifier = new AttributeModifier(modifierId, modifierName, value, AttributeModifier.Operation.ADD_NUMBER);
                instance.addModifier(modifier);
            }
        }
    }

    private void clearPlayerModifiers(Player player) {
        // 성능 최적화
        clearAttribute(player, Attribute.GENERIC_MAX_HEALTH, "lore_health");
        clearAttribute(player, Attribute.GENERIC_ATTACK_DAMAGE, "lore_attack");
        clearAttribute(player, Attribute.GENERIC_ARMOR, "lore_armor");
        clearAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, "lore_speed");
    }

    private void clearAttribute(Player player, Attribute attribute, String modifierName) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            UUID modifierId = UUID.nameUUIDFromBytes(modifierName.getBytes());
            Collection<AttributeModifier> modifiers = new ArrayList<>(instance.getModifiers());
            for (AttributeModifier mod : modifiers) {
                if (mod.getUniqueId().equals(modifierId)) {
                    instance.removeModifier(mod);
                }
            }
        }
    }

    private void handleRegeneration(Player player, double regenValue) {
        if (regenValue > 0) {
            startRegenForPlayer(player, regenValue);
        } else {
            stopRegenForPlayer(player);
        }
    }

    private void startRegenForPlayer(Player player, double regenRate) {
        stopRegenForPlayer(player);

        int interval = plugin.getConfig().getInt("regeneration.interval", 20);
        double healPerPoint = plugin.getConfig().getDouble("regeneration.heal-per-point", 1.0);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                double currentHealth = player.getHealth();
                AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (healthAttr == null) return;

                double maxHealth = healthAttr.getValue();
                if (currentHealth < maxHealth) {
                    double newHealth = Math.min(maxHealth, currentHealth + (regenRate * healPerPoint));
                    player.setHealth(newHealth);
                }
            }
        };

        task.runTaskTimer(plugin, interval, interval);
        regenTasks.put(player.getUniqueId(), task);
    }

    private void stopRegenForPlayer(Player player) {
        BukkitRunnable task = regenTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void startRegenTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Map<StatType, Double> stats = playerStats.get(player.getUniqueId());
                    if (stats != null) {
                        double regenValue = stats.get(StatType.재생력);
                        if (regenValue > 0) {
                            
                            handleRegeneration(player, regenValue);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 100L); 
    }

    public Map<StatType, Double> getPlayerStats(UUID playerId) {
        return playerStats.getOrDefault(playerId, createEmptyStatMap());
    }

    private Map<StatType, Double> createEmptyStatMap() {
        Map<StatType, Double> stats = new EnumMap<>(StatType.class);
        for (StatType type : StatType.values()) {
            stats.put(type, 0.0);
        }
        return stats;
    }

    
    private boolean isValidWeapon(ItemStack item) {
        if (item == null) return false;
        if (!cacheInitialized) initializeCache();
        return validWeapons.contains(item.getType());
    }

    private boolean isValidArmor(ItemStack item) {
        if (item == null) return false;
        if (!cacheInitialized) initializeCache();
        return validArmor.contains(item.getType());
    }

    
    private String getLoreFormat(StatType statType) {
        return plugin.getConfig().getString("lore-format." + statType.getDisplayName(),
            "&6[능력치] &f" + statType.getDisplayName() + " +%value%");
    }

    // 숫자 추출 최적화
    private String extractNumber(String text) {
        StringBuilder sb = new StringBuilder();
        boolean foundDot = false;
        for (char c : text.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (c == '.' && !foundDot) {
                sb.append(c);
                foundDot = true;
            } else if (sb.length() > 0) {
                break;
            }
        }
        return sb.toString();
    }

    private void logPlayerStats(Player player, Map<StatType, Double> stats) {
        plugin.getLogger().info("플레이어 " + player.getName() + "의 능력치 업데이트:");
        for (StatType type : StatType.values()) {
            double value = stats.get(type);
            if (value > 0) {
                plugin.getLogger().info("  " + type.getDisplayName() + ": " + value);
            }
        }
    }

    public void reloadConfig() {
        initializeCache();
    }

    public void shutdown() {
        regenTasks.values().forEach(BukkitRunnable::cancel);
        regenTasks.clear();
        playerStats.clear();
        validWeapons.clear();
        validArmor.clear();
    }
}