package ru.atlantacraft.atlantarpplugin;

import org.bukkit.plugin.java.JavaPlugin;
import ru.atlantacraft.atlantarpplugin.commands.CommandCallEmg;
import ru.atlantacraft.atlantarpplugin.commands.CommandCapture;
import ru.atlantacraft.atlantarpplugin.commands.CommandRespondEmg;
import ru.atlantacraft.atlantarpplugin.commands.CommandToggleCapturable;

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
    }

    @Override
    public void onDisable() {
        CommandCapture.inst().save_capture_data();
        CommandToggleCapturable.inst().save();
    }
}
