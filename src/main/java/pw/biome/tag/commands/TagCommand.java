package pw.biome.tag.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pw.biome.tag.Tag;
import pw.biome.tag.database.DatabaseHelper;
import pw.biome.tag.object.TagItem;
import pw.biome.tag.object.TagPlayer;

@CommandAlias("tag")
@Description("Tag commands")
public class TagCommand extends BaseCommand {

    @Default
    @Description("Shows the tagged player")
    public void onTag(CommandSender sender) {
        if (!Tag.isGameRunning()) {
            sender.sendMessage(ChatColor.GOLD + "There is currently no running tag game!");
        } else {
            DatabaseHelper.getCurrentTaggedPlayer().thenAccept(username -> sender.sendMessage(ChatColor.GOLD +
                    "Current tagged player: " + ChatColor.RED + username));
        }
    }

    @Subcommand("stop")
    @CommandPermission("tag.admin")
    @Description("Stops the current tag game")
    public void onTagStop(Player player) {
        if (Tag.isGameRunning()) {
            Tag.getInstance().stopGame();
            Bukkit.broadcastMessage(ChatColor.GOLD + "Tag has been stopped!");
        } else {
            player.sendMessage(ChatColor.RED + "There is no tag game running");
        }
    }

    @Subcommand("start")
    @CommandPermission("tag.admin")
    @Description("Stops the current tag game")
    public void onTagStart(Player player) {
        if (!Tag.isGameRunning()) {
            Tag.getInstance().startGame();
            Bukkit.broadcastMessage(ChatColor.GOLD + "Tag has been started!");
        } else {
            player.sendMessage(ChatColor.RED + "There is no tag game running!");
        }
    }


    @Subcommand("give")
    @CommandPermission("tag.admin")
    @Description("Gives the player a tag item")
    public void onTagGive(Player player) {
        TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());
        DatabaseHelper.removeTaggedPlayerAndAddTo(tagPlayer);
        player.sendMessage(ChatColor.GREEN + "Giving tag to '" + tagPlayer.getUsername() + ChatColor.GREEN + "'!");
    }

    @Subcommand("force")
    @CommandPermission("tag.admin")
    @CommandCompletion("@players")
    @Description("Forces the tagged player to the given target")
    public void onTagForce(CommandSender sender, OnlinePlayer target) {
        TagPlayer tagPlayer = TagPlayer.getFromUUID(target.getPlayer().getUniqueId());
        if (tagPlayer != null) {
            DatabaseHelper.removeTaggedPlayerAndAddTo(tagPlayer);
            tagPlayer.setTimesTagged(tagPlayer.getTimesTagged() - 1);
            sender.sendMessage(ChatColor.GREEN + "Forcing '" + tagPlayer.getUsername() + ChatColor.GREEN + "' as tagged!");
        }
    }

    @Subcommand("item")
    @CommandPermission("tag.admin")
    @Description("Gives the requesting player the Tag item")
    public void onTagItem(Player player) {
        player.getInventory().addItem(TagItem.getTagItem());
        player.sendMessage(ChatColor.GREEN + "Here's the tag!");
    }

    @Subcommand("debug")
    @CommandPermission("tag.admin")
    public void debug(Player player) {
        player.sendMessage(ChatColor.GREEN + "Tag game is running? " + Tag.isGameRunning());
    }
}
