package ru.atlantacraft.atlantarpplugin.commands;

import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.RegionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import ru.atlantacraft.atlantarpplugin.AtlantaRPPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PayingRegionManager implements CommandExecutor, Listener {

    private static PayingRegionManager inst;
    private static ConcurrentHashMap<String, RegionData> regions = new ConcurrentHashMap<>();
    private static int reward_period = 100;

    public static PayingRegionManager inst(){
        return inst;
    }

    public PayingRegionManager(){
        inst = this;
        load();
        reward_period = AtlantaRPPlugin.inst().getConfig().getInt("reward_period");
        AtlantaRPPlugin.inst().getServer().getScheduler().scheduleSyncRepeatingTask(
            AtlantaRPPlugin.inst(),
            ()->{
                for(Map.Entry<String, RegionData> data: regions.entrySet()){
                    inst().reward(data.getKey(), data.getValue());
                }
            },
            100,
            reward_period);
        //периодическое сохранение
        AtlantaRPPlugin.inst().getServer().getScheduler().scheduleSyncRepeatingTask(
                AtlantaRPPlugin.inst(),
                this::save,
                100,
                12000);
    }

    private static final String pay_file = "paying_regions.yml";

    public void load(){
        File capture_data = new File(AtlantaRPPlugin.inst().getDataFolder(), pay_file);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(capture_data);
        for(String key : config.getKeys(false)){
            String owner_uuid = config.getString(key+".owner");
            int amount = config.getInt(key+".amount");
            String world = config.getString(key+".world");
            regions.put(key, new RegionData(amount, owner_uuid, world));
        }
    }

    public void save(){
        YamlConfiguration config = new YamlConfiguration();
        for(Map.Entry<String, RegionData> entry : regions.entrySet()){
            config.set(entry.getKey()+".owner", entry.getValue().owner.toString());
            config.set(entry.getKey()+".amount", entry.getValue().pay_amount);
            config.set(entry.getKey()+".world", entry.getValue().world);
        }
        File capture_data = new File(AtlantaRPPlugin.inst().getDataFolder(), "capture.yml");
        capture_data.delete();
        try {
            config.save(capture_data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reward(String reg_name, RegionData region){
        RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
        RegionManager manager = container.get(Bukkit.getWorld(region.world));
        ProtectedRegion reg = manager.getRegions().get(reg_name);
        if(reg == null){
            regions.remove(reg_name);
            return;
        }
        //ВАЖНО - зависит от команды /givecoins из мода universal coins
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
                "givecoins " + region.owner + " " + region.pay_amount);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length < 1){
            return false;
        }
        String subcom = args[0];
        if(subcom.equals("add")){
            return addRegSubc(sender, command, label, args);
        }else if(subcom.equals("remove")){
            return removeRegSubc(sender, command, label, args);
        }else if(subcom.equals("query")){
            return queryReg(sender, command, label, args);
        }else if(subcom.equals("setowner")){
            return setOwnerSubc(sender, command, label, args);
        }else if(subcom.equals("setpay")){
            return setPaySubc(sender, command, label, args);
        }
        return false;
    }

    private boolean addRegSubc(CommandSender sender, Command command, String label, String[] args){
        if(args.length < 3){
            return false;
        }
        String region = args[1];
        int pay;
        try{
            pay = Integer.parseInt(args[2]);
        }
        catch(NumberFormatException e){
            return false;
        }

        ProtectedRegion reg;
        if(!(sender instanceof Player)){
            return false;
        }
        if(region == "-"){
            RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
            RegionQuery query = container.createQuery();
            Iterator<ProtectedRegion> regs = query.getApplicableRegions(((Player)sender).getLocation()).iterator();
            if(regs.hasNext()){
                reg = regs.next();
            }else{
                return false;
            }
        }else{
            RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
            RegionManager manager = container.get(((Player)sender).getWorld());
            reg = manager.getRegions().get(region);
        }
        if(reg == null){
            return false;
        }
        regions.put(region, new RegionData(pay, null, ((Player) sender).getWorld().getName()));
        return true;
    }

    public boolean addReg(String reg_name, int pay, String world_name){
        RegionContainer container = WorldGuardPlugin.inst().getRegionContainer();
        RegionManager manager = container.get(Bukkit.getWorld(world_name));
        ProtectedRegion reg = manager.getRegions().get(reg_name);
        if(reg == null){
            return false;
        }
        regions.put(reg_name, new RegionData(pay, null, world_name));
        return true;
    }

    private boolean removeRegSubc(CommandSender sender, Command command, String label, String[] args){
        if(args.length < 2){
            return false;
        }
        String region = args[1];
        regions.remove(region);
        return true;
    }

    public boolean removeReg(String reg_name){
        return regions.remove(reg_name) != null;
    }

    private boolean queryReg(CommandSender sender, Command command, String label, String[] args){
        if(args.length < 2){
            return false;
        }
        String region = args[1];
        RegionData data = regions.get(region);
        if(data == null){
            sender.sendMessage("Нет такого региона");
        }else{
            sender.sendMessage(data.toString());
        }
        return true;
    }

    private boolean setOwnerSubc(CommandSender sender, Command command, String label, String[] args){
        if(args.length < 3){
            return false;
        }
        String region = args[1];
        Player owner = Bukkit.getPlayer(args[2]);
        if(owner == null){
            return false;
        }
        RegionData data = regions.get(region);
        if(data == null){
            return false;
        }
        data.owner = owner.getName();
        return true;
    }

    public boolean setOwner(String reg_name, String owner_name){
        RegionData data = regions.get(reg_name);
        if(data!=null){
            data.owner = owner_name;
        }else{
            return false;
        }
        return true;
    }

    private boolean setPaySubc(CommandSender sender, Command command, String label, String[] args){
        if(args.length < 3){
            return false;
        }
        String region = args[1];
        int pay;
        try{
            pay = Integer.parseInt(args[2]);
        } catch (NumberFormatException e){
            return false;
        }
        RegionData data = regions.get(region);
        if(data == null){
            return false;
        }
        data.pay_amount = pay;
        return true;
    }

    public static class RegionData{
        public int pay_amount;
        public String owner;
        public String world;

        @Override
        public String toString() {
            return "RegionData{" +
                    "pay_amount=" + pay_amount +
                    ", owner=" + owner +
                    ", world='" + world + '\'' +
                    '}';
        }

        public RegionData(int pay_amount, String owner, String world){
            this.pay_amount = pay_amount;
            this.owner = owner;
            this.world = world;
        }
    }
}
