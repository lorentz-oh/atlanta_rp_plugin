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
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import javax.swing.plaf.synth.Region;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PayingRegionManager implements CommandExecutor, Listener {

    private static PayingRegionManager inst;
    private static ConcurrentHashMap<String, RegionData> regions = new ConcurrentHashMap<>();

    public static PayingRegionManager inst(){
        return inst;
    }

    public PayingRegionManager(){
        inst = this;
        
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length < 1){
            return false;
        }
        String subcom = args[0];
        if(subcom.equals("add")){
            return addReg(sender, command, label, args);
        }else if(subcom.equals("remove")){
            return removeReg(sender, command, label, args);
        }else if(subcom.equals("query")){
            return queryReg(sender, command, label, args);
        }else if(subcom.equals("setowner")){
            return setOwner(sender, command, label, args);
        }else if(subcom.equals("setpay")){
            return setPay(sender, command, label, args);
        }
        return false;
    }

    private boolean addReg(CommandSender sender, Command command, String label, String[] args){
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
        regions.put(region, new RegionData(pay, null));
        return true;
    }

    private boolean removeReg(CommandSender sender, Command command, String label, String[] args){
        if(args.length < 2){
            return false;
        }
        String region = args[1];
        regions.remove(region);
        return true;
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

    private boolean setOwner(CommandSender sender, Command command, String label, String[] args){
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
        data.owner = owner.getUniqueId();
        return true;
    }

    private boolean setPay(CommandSender sender, Command command, String label, String[] args){
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
        public UUID owner;

        @Override
        public String toString() {
            return "RegionData{" +
                    "pay_amount=" + pay_amount +
                    ", owner=" + owner +
                    '}';
        }

        public RegionData(int pay_amount, UUID owner){
            this.pay_amount = pay_amount;
            this.owner = owner;
        }
    }
}
