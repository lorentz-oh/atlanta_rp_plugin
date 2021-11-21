package ru.atlantacraft.atlantarpplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.UUID;

public class PayingRegionManager implements CommandExecutor, Listener {

    private static PayingRegionManager inst;
    private static HashMap<String, RegionData> regions;

    public static PayingRegionManager inst(){
        return inst;
    }

    public PayingRegionManager(){
        inst = this;

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    public static class RegionData{
        public int pay_amount;
        public UUID owner;

        public RegionData(int pay_amount, UUID owner){
            this.pay_amount = pay_amount;
            this.owner = owner;
        }
    }
}
