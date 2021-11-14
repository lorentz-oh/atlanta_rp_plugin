package ru.atlantacraft.atlantarpplugin.commands;

import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.RegionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import ru.atlantacraft.atlantarpplugin.AtlantaRPPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

public class CommandToggleCapturable implements CommandExecutor, Listener {
    private static CommandToggleCapturable instance;
    private HashSet<String> capturable_regs = new HashSet<>();

    public CommandToggleCapturable(){
        instance = this;
        //периодическое сохранение
        AtlantaRPPlugin.inst().getServer().getScheduler().scheduleSyncRepeatingTask(
                AtlantaRPPlugin.inst(),
                ()->{inst().save();},
                100,
                12000);
    }

    public static CommandToggleCapturable inst(){
        return instance;
    }

    public void setCapturable(String region, boolean state){
        if(state){
            capturable_regs.add(region);
        }else{
            capturable_regs.remove(region);
        }
    }

    public boolean isCapturable(String region){
        return capturable_regs.contains(region);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String region_name;
        if(args.length == 1){
            region_name = args[0];
        } else if(args.length == 0 && sender instanceof Player){
            RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
            RegionQuery query = container.createQuery();
            Iterator<ProtectedRegion> regs = query.getApplicableRegions(((Player)sender).getLocation()).iterator();
            if(regs.hasNext()){
                ProtectedRegion reg = regs.next();
                region_name = reg.getId();
            }else{
                return false;
            }
        } else{
            return false;
        }
        boolean state = isCapturable(region_name);
        state = !state;
        setCapturable(region_name, state);
        if(state){
            sender.sendMessage("Регион теперь можно захватить");
        }else{
            sender.sendMessage("Регион больше нельзя захватить");
        }
        return true;
    }

    @EventHandler
    public void onLoad(PluginEnableEvent event){
        File capture_data = new File(AtlantaRPPlugin.inst().getDataFolder(), "capturable.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(capture_data);
        for(String key : config.getKeys(false)){
            setCapturable(key, true);
        }
    }

    public void save(){
        YamlConfiguration config = new YamlConfiguration();
        for(String key : capturable_regs){
            config.set(key, "");
        }
        File capturable_data = new File(AtlantaRPPlugin.inst().getDataFolder(), "capturable.yml");
        capturable_data.delete();
        try {
            config.save(capturable_data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
