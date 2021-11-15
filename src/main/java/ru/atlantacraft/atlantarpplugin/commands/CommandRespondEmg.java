package ru.atlantacraft.atlantarpplugin.commands;

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
        Event.Result result = CommandCallEmg.inst().respondCall((Player)sender, id);
        switch (result){
            case DENY:
                sender.sendMessage("Не получилось отреагировать на вызов. Возможно, он уже просрочился.");
                return true;
            case DEFAULT:
                return false;
            case ALLOW:
                sender.sendMessage("Вы отрегировали на вызов");
                return true;
        }
        return false;
    }
}
