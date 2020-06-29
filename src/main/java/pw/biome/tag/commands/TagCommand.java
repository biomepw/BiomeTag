package pw.biome.tag.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.biome.tag.database.DatabaseHelper;
import pw.biome.tag.object.TagItem;
import pw.biome.tag.object.TagPlayer;

public class TagCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            DatabaseHelper.getCurrentTaggedPlayer().thenAccept(username -> sender.sendMessage(ChatColor.GOLD +
                    "Current tagged player: " + ChatColor.RED + username));
        }

        if (args.length > 0) {
            if (sender instanceof Player && sender.hasPermission("tag.admin")) {
                Player player = (Player) sender;

                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("give")) {
                        TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());
                        DatabaseHelper.removeTaggedPlayerAndAddTo(tagPlayer);
                    } else if (args[0].equalsIgnoreCase("item")) {
                        player.getInventory().addItem(TagItem.getTagItem());
                    } else if (args[0].equalsIgnoreCase("sync")) {
                        DatabaseHelper.syncData().thenRun(() -> player.sendMessage(ChatColor.GREEN + "Data has been synchronised"));
                    }
                } else if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("force")) {
                        TagPlayer tagPlayer = TagPlayer.getFromName(args[1]);

                        if (tagPlayer != null) {
                            DatabaseHelper.removeTaggedPlayerAndAddTo(tagPlayer);

                            tagPlayer.setTimesTagged(tagPlayer.getTimesTagged() - 1);
                        }
                    }
                }
            }
        }
        return true;
    }
}
