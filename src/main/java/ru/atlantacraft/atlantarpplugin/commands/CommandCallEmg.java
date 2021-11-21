package ru.atlantacraft.atlantarpplugin.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.atlantacraft.atlantarpplugin.AtlantaRPPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/*Данная команда вызывает экстренные службы, или такси. В общем всех, кто может отреагировать на вызов*/
public class CommandCallEmg implements CommandExecutor, Listener {
    private static CommandCallEmg inst;
    private static final String RESV_PERM_PRE = "atl.resvemg.";
    private static int call_life_ticks;
    private static int LIFE_UPDT_TICKS = 200;

    ConcurrentHashMap<Integer, Call> calls = new ConcurrentHashMap<>();

    public static CommandCallEmg inst(){
        return inst;
    }

    public CommandCallEmg(){
        inst = this;

        call_life_ticks = AtlantaRPPlugin.inst().getConfig().getInt("call_life_ticks");
        AtlantaRPPlugin.inst().getServer().getScheduler().scheduleSyncRepeatingTask(
                AtlantaRPPlugin.inst(),
                ()->{inst.removeOldCalls(LIFE_UPDT_TICKS);},
                100,
                LIFE_UPDT_TICKS);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String group;
        StringBuilder mesg = new StringBuilder();
        if(args.length > 0){
            group = args[0];
        }else{
            return false;
        }
        if(args.length>1){
            for (int i = 1; i < args.length; i++) {
                mesg.append(args[i]);
                if(i<(args.length-1)){
                    mesg.append(' ');
                }
            }
        }
        if(!(sender instanceof Player)){
            return false;
        }
        makeCall(new Call(mesg.toString(), group, ((Player) sender).getUniqueId(), ((Player) sender).getLocation()), (Player) sender);
        return true;
    }

    private int genId(){
        int id = 0;
        while(calls.containsKey(id)){
            id++;
        }
        return id;
    }

    private void makeCall(Call call, Player caller){
        int id = genId();
        calls.put(id, call);
        for(Player player: Bukkit.getOnlinePlayers()){
            if(player.hasPermission(RESV_PERM_PRE+call.group)){
                int distance = (int) player.getLocation().distance(caller.getLocation());
                BaseComponent[] comp = new ComponentBuilder("")
                        .append("Вы получили вызов №" + id +": \""+call.mesg+"\", расстояние: "+distance+" м, группа: "+call.group+". ").color(ChatColor.GREEN)
                        .append("Принять").color(ChatColor.BLUE)
                        .underlined(true).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/respondemg " + id)).create();
                player.spigot().sendMessage(comp);
            }
        }
    }

    public Call respondCall(Player player, int id){
        Call call = calls.get(id);
        if(call == null || !player.hasPermission(RESV_PERM_PRE+call.group)){
            return null;
        }
        Player caller = Bukkit.getPlayer(call.caller);
        if(caller == null){
            return null;
        }
        caller.sendMessage("На ваш вызов отреагировали");
        calls.remove(id);
        return call;
    }

    private void removeOldCalls(int update_period){
        ArrayList<Integer> old_calls = new ArrayList<>();
        for(Map.Entry<Integer, Call> entry: calls.entrySet()){
            entry.getValue().ticksLived += update_period;
            if(entry.getValue().ticksLived > call_life_ticks){
                old_calls.add(entry.getKey());
            }
        }
        for(Integer call: old_calls){
            calls.remove(call);
        }
    }

    public static class Call{
        public Call(String mesg, String group, UUID caller, Location loc){
            this.mesg = mesg;
            this.group = group;
            this.caller = caller;
            this.loc = loc;
        }

        public Location loc;
        public String mesg;
        public String group;
        public UUID caller;
        public int ticksLived = 0;
    }
}
