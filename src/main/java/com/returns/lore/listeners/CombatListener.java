package com.returns.lore.listeners;

import com.returns.lore.LorePlugin;
import com.returns.lore.utils.StatType;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;

public class CombatListener implements Listener {

    private final LorePlugin plugin;
    private final Random random = new Random();

    public CombatListener(LorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;
        if (event.isCancelled()) return;

        Map<StatType, Double> defenderStats = plugin.getStatManager().getPlayerStats(defender.getUniqueId());
        double dodgeChance = defenderStats.getOrDefault(StatType.회피율, 0.0);

        if (dodgeChance > 0 && random.nextDouble() * 100 < dodgeChance) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;

        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof Player defender) {
            handlePlayerVsPlayerDamage(event, attacker, defender);
        } else if (event.getDamager() instanceof Player attacker) {
            handlePlayerVsEntityDamage(event, attacker);
        }
    }

    private void handlePlayerVsPlayerDamage(EntityDamageByEntityEvent event, Player attacker, Player defender) {
        Map<StatType, Double> attackerStats = plugin.getStatManager().getPlayerStats(attacker.getUniqueId());
        Map<StatType, Double> defenderStats = plugin.getStatManager().getPlayerStats(defender.getUniqueId());

        double damage = event.getDamage();

        double critChance = attackerStats.getOrDefault(StatType.치명타확률, 0.0);
        double critDamage = attackerStats.getOrDefault(StatType.치명타데미지, 0.0);
        double armorPen = attackerStats.getOrDefault(StatType.방어구관통력, 0.0);
        double lifeStealRate = attackerStats.getOrDefault(StatType.흡혈률, 0.0);
        double lifeStealAmount = attackerStats.getOrDefault(StatType.흡혈력, 0.0);

        boolean isCrit = critChance > 0 && random.nextDouble() * 100 < critChance;
        if (isCrit) {
            damage += critDamage;
        }

        if (armorPen > 0) {
            double defenderArmor = defender.getAttribute(Attribute.GENERIC_ARMOR).getValue();
            double penetratedArmor = Math.min(defenderArmor, armorPen);
            double armorReduction = 1.0 - (defenderArmor - penetratedArmor) / (defenderArmor + 20.0);
            damage *= (1.0 + armorReduction * 0.1);
        }

        event.setDamage(damage);

        if (lifeStealRate > 0 && random.nextDouble() * 100 < lifeStealRate) {
            double healAmount = lifeStealAmount > 0 ? lifeStealAmount : damage * 0.1;
            double currentHealth = attacker.getHealth();
            double maxHealth = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double newHealth = Math.min(maxHealth, currentHealth + healAmount);

            attacker.setHealth(newHealth);
        }
    }

    private void handlePlayerVsEntityDamage(EntityDamageByEntityEvent event, Player attacker) {
        Map<StatType, Double> attackerStats = plugin.getStatManager().getPlayerStats(attacker.getUniqueId());

        double damage = event.getDamage();

        double critChance = attackerStats.getOrDefault(StatType.치명타확률, 0.0);
        double critDamage = attackerStats.getOrDefault(StatType.치명타데미지, 0.0);
        double lifeStealRate = attackerStats.getOrDefault(StatType.흡혈률, 0.0);
        double lifeStealAmount = attackerStats.getOrDefault(StatType.흡혈력, 0.0);

        boolean isCrit = critChance > 0 && random.nextDouble() * 100 < critChance;
        if (isCrit) {
            damage += critDamage;
        }

        event.setDamage(damage);

        if (lifeStealRate > 0 && random.nextDouble() * 100 < lifeStealRate) {
            double healAmount = lifeStealAmount > 0 ? lifeStealAmount : damage * 0.1;
            double currentHealth = attacker.getHealth();
            double maxHealth = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double newHealth = Math.min(maxHealth, currentHealth + healAmount);

            attacker.setHealth(newHealth);
        }
    }
}