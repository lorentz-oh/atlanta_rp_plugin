package ru.atlantacraft.atlantarpplugin.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class CommandRespondEmg implements CommandExecutor {
    private static CommandRespondEmg inst;

    public static CommandRespondEmg inst(){
        return inst;
    }

    public CommandRespondEmg(){
        inst = this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)){
            return false;
        }
        int id = 0;
        if(args.length == 1){
            try{
            id = Integer.parseInt(args[0]);}
            catch (NumberFormatException e){
                return false;
            }
        }
        CommandCallEmg.Call call = CommandCallEmg.inst().respondCall((Player)sender, id);
        if(call == null){
            sender.spigot().sendMessage(
                    new ComponentBuilder("Не получилось отреагировать на вызов. Возможно, он уже просрочился.")
                            .color(ChatColor.GREEN).create()
            );
        }else{
            sender.spigot().sendMessage(
                new ComponentBuilder("Вы отрегировали, нажмите чтобы поставить метку: " + formatLoc(call.loc))
                        .color(ChatColor.GREEN).create()
            );

        }
        return true;
    }

    public static String formatLoc(Location loc){
        return "[x:" + loc.getBlockX()+", y:"+ loc.getBlockY()+", z:"+ loc.getBlockZ()+"]";
    }
}
