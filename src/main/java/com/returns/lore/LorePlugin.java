package com.returns.lore;

import com.returns.lore.commands.LoreCommand;
import com.returns.lore.commands.StatsCommand;
import com.returns.lore.listeners.EquipmentListener;
import com.returns.lore.listeners.CombatListener;
import com.returns.lore.managers.StatManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LorePlugin extends JavaPlugin {

    private StatManager statManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.statManager = new StatManager(this);

        registerCommands();
        registerListeners();

        getLogger().info("LorePlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (statManager != null) {
            statManager.shutdown();
        }
        getLogger().info("LorePlugin has been disabled!");
    }

    private void registerCommands() {
        LoreCommand loreCommand = new LoreCommand(this);
        StatsCommand statsCommand = new StatsCommand(this);

        getCommand("로어").setExecutor(loreCommand);
        getCommand("로어").setTabCompleter(loreCommand);
        getCommand("스텟").setExecutor(statsCommand);
        getCommand("스텟").setTabCompleter(statsCommand);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new EquipmentListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
    }

    public StatManager getStatManager() {
        return statManager;
    }
}