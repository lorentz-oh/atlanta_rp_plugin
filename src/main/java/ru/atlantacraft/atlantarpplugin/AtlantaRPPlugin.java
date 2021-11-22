package ru.atlantacraft.atlantarpplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import ru.atlantacraft.atlantarpplugin.commands.*;

import java.util.UUID;

public final class AtlantaRPPlugin extends JavaPlugin {

    private static AtlantaRPPlugin inst;

    public AtlantaRPPlugin(){
        inst = this;
    }

    public static AtlantaRPPlugin inst(){
        return inst;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.getCommand("capture").setExecutor(new CommandCapture());
        this.getServer().getPluginManager().registerEvents(CommandCapture.inst(), this);

        this.getCommand("togglecapturable").setExecutor(new CommandToggleCapturable());
        this.getServer().getPluginManager().registerEvents(CommandToggleCapturable.inst(), this);

        this.getCommand("callemg").setExecutor(new CommandCallEmg());
        this.getServer().getPluginManager().registerEvents(CommandCallEmg.inst(), this);

        this.getCommand("respondemg").setExecutor(new CommandRespondEmg());

        this.getCommand("payregion").setExecutor(new PayingRegionManager());
        this.getServer().getPluginManager().registerEvents(PayingRegionManager.inst(), this);
    }

    @Override
    public void onDisable() {
        CommandCapture.inst().save_capture_data();
        CommandToggleCapturable.inst().save();
        PayingRegionManager.inst().save();
    }
}
