package ru.atlantacraft.atlantarpplugin.commands;

import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.RegionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.server.PluginEnableEvent;
import ru.atlantacraft.atlantarpplugin.AtlantaRPPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CommandCapture implements CommandExecutor, Listener {

    private HashMap<String, CaptureData> capturing = new HashMap<>();

    private static int capture_ticks = 200;
    private static int capture_upd_ticks = 20;

    private static CommandCapture inst;

    public static CommandCapture inst(){
        return inst;
    }

    public CommandCapture(){
        inst = this;

        capture_ticks = AtlantaRPPlugin.inst().getConfig().getInt("capture_ticks");
        capture_upd_ticks = AtlantaRPPlugin.inst().getConfig().getInt("capture_upd_ticks");

        if(capture_upd_ticks == 0){
            AtlantaRPPlugin.inst().getLogger().warning("capture_upd_ticks установлен на ноль, меняем");
            capture_upd_ticks = 60;
        }
        AtlantaRPPlugin.inst().getLogger().info(
                "capture_ticks = " + capture_ticks +
        "; capture_upd_ticks = " + capture_upd_ticks);
        AtlantaRPPlugin.inst().getServer().getScheduler().scheduleSyncRepeatingTask(
                AtlantaRPPlugin.inst(),
                ()->{inst.onUpdate();},
                100,
                capture_upd_ticks);

        AtlantaRPPlugin.inst().getServer().getScheduler().scheduleSyncRepeatingTask(
                AtlantaRPPlugin.inst(),
                ()->{inst.save_capture_data();},
                100,
                2400);
    }

    private void load_capture_data(){
        File capture_data = new File(AtlantaRPPlugin.inst().getDataFolder(), "capture.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(capture_data);
        ConfigurationSection section = config.getConfigurationSection("capturing");
        for(String region : section.getKeys(false)){
            if(section.isString("uuid") && section.isInt("time_left")){
                UUID uuid = UUID.fromString(section.getString("uuid"));
                int time_left = section.getInt("time_left");
                capturing.put(region, new CaptureData(uuid, time_left));
            }
        }
    }

    public void save_capture_data(){
        YamlConfiguration config = new YamlConfiguration();
        for(Map.Entry<String, CaptureData> entry: capturing.entrySet()){
            String prefix = "capturing."+entry.getKey();
            config.set(prefix+".uuid", entry.getValue().player_uuid.toString());
            config.set(prefix+".time_left", entry.getValue().time_left);
        }

        File capture_data = new File(AtlantaRPPlugin.inst().getDataFolder(), "capture.yml");
        capture_data.delete();
        try {
            config.save(capture_data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onLoad(PluginEnableEvent event){
        if(event.getPlugin() == AtlantaRPPlugin.inst()){
            AtlantaRPPlugin.inst().getLogger().info("Loading capture data");
            try{
                load_capture_data();}
            catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event){
        for(Map.Entry<String, CaptureData> entry: capturing.entrySet()){
            if(entry.getValue().player_uuid == event.getEntity().getUniqueId()){
                failCapture(entry.getKey());
            }
        }
    }

    public void onUpdate(){
        for(Map.Entry<String, CaptureData> entry: capturing.entrySet()){
            Player player = Bukkit.getServer().getPlayer(entry.getValue().player_uuid);
            if(player == null){
                failCapture(entry.getKey());
            }
            //проверяем находится ли игрок в захватываемом регионе
            RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
            RegionQuery query = container.createQuery();
            Iterator<ProtectedRegion> regs = query.getApplicableRegions(player.getLocation()).iterator();
            ProtectedRegion reg;
            if(regs.hasNext()){
                reg = regs.next();
            }else{
                failCapture(entry.getKey());
                continue;
            }
            if(reg.getId() != entry.getKey()){
                failCapture(entry.getKey());
                continue;
            }
            entry.getValue().time_left -= capture_upd_ticks;
            if(entry.getValue().time_left <= 0){
                capture(entry.getKey(), player);
            }
        }
    }

    private void capture(String region, Player player){
        String owner_name = PayingRegionManager.inst().getOwner(region);
        if(owner_name != null){
            Player owner = Bukkit.getPlayer(owner_name);
            if(owner != null){
                owner.sendMessage("Ваш регион " + region + " был захвачен " + player.getName());
            }
        }
        player.sendMessage("Вы захватили " + region);
        capturing.remove(region);
        PayingRegionManager.inst().setOwner(region, player.getName());
    }

    private void failCapture(String region){
        CaptureData data = capturing.get(region);
        if(data == null){
            return;}
        Player player = Bukkit.getPlayer(data.player_uuid);
        if(player != null){
            player.sendMessage("Вы провалили захват");
        }
        capturing.remove(region);
        String owner_name = PayingRegionManager.inst().getOwner(region);
        if(owner_name != null){
            Player owner = Bukkit.getPlayer(owner_name);
            if(owner != null){
                owner.sendMessage("Вы изгнали захватчиков");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player;
        if(sender instanceof Player){
            player = (Player) sender;}
        else{
            return false;
        }
        RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
        RegionQuery query = container.createQuery();
        Iterator<ProtectedRegion> regs = query.getApplicableRegions(player.getLocation()).iterator();
        if(regs.hasNext()){
            ProtectedRegion reg = regs.next();
            tryCapture(player, reg);
        }else{
            player.sendMessage("Здесь нет регионов");
        }

        return true;
    }

    private void tryCapture(Player player, ProtectedRegion region){
        if(!CommandToggleCapturable.inst().isCapturable(region.getId())){
            player.sendMessage("Этот регион нельзя захватить");
            return;
        }
        CaptureData capturingData = capturing.get(region.getId());
        if(capturingData != null){
            if(capturingData.player_uuid == player.getUniqueId()){
                player.sendMessage("Вы уже захватываете этот регион");
            }else{
                player.sendMessage("Этот регион уже захватывается");
            }
            return;
        }
        String owner_name = PayingRegionManager.inst().getOwner(region.getId());
        if(owner_name != null){
            if(owner_name == player.getName()){
                player.sendMessage("Этот регион уже захвачен вами");
                return;
            }else{
                Player other_player = Bukkit.getPlayer(owner_name);
                if(other_player!=null){
                    other_player.sendMessage("Ваш регион " + region.getId() + " начал захватывать " + player.getName());
                }
            }
        }

        player.sendMessage("Вы начали захватывать регион");
        capturing.put(region.getId(), new CaptureData(player.getUniqueId()));
    }

    public static class CaptureData{
        public CaptureData(UUID uuid){
            player_uuid = uuid;
        }

        public CaptureData(UUID uuid, int time) {
            player_uuid = uuid;
            time_left = time;
        }

        public UUID player_uuid;
        public int time_left = capture_ticks;

    }
}
