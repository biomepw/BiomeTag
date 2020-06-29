package pw.biome.tag;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pro.husk.ichat.iChat;
import pro.husk.ichat.obj.PlayerCache;
import pw.biome.tag.commands.TagCommand;
import pw.biome.tag.database.DatabaseHelper;
import pw.biome.tag.listener.TagListener;
import pw.biome.tag.object.TagItem;
import pw.biome.tag.object.TagPlayer;

public class Tag extends JavaPlugin {

    @Getter
    private static Tag instance;

    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        DatabaseHelper.setupDatabase();
        TagItem.initiateItem();

        getCommand("tag").setExecutor(new TagCommand());
        getServer().getPluginManager().registerEvents(new TagListener(), this);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, this::updateScoreboards, (10 * 20), (10 * 20));
    }

    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());
            tagPlayer.saveToDatabase();
        }
    }

    public void updateScoreboards() {
        // We want to overwrite the updateScoreboard of iChat
        int scoreboardTaskId = iChat.getScoreboardTaskId();
        if (scoreboardTaskId != 0) {
            Bukkit.getScheduler().cancelTask(iChat.getScoreboardTaskId());
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            ImmutableList<Player> playerList = ImmutableList.copyOf(getServer().getOnlinePlayers());
            for (Player player : playerList) {
                TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());
                PlayerCache playerCache = PlayerCache.getFromUUID(player.getUniqueId());

                boolean afk = tagPlayer.isAFK();

                if (playerCache != null) {
                    int amountOfTimeTagged = tagPlayer.getAmountOfTimeTagged() / 60;
                    ChatColor prefix = playerCache.getRank().getPrefix();

                    if (tagPlayer.isTagged()) prefix = ChatColor.DARK_RED;

                    if (afk) {
                        player.setPlayerListName(ChatColor.GRAY + player.getName() + ChatColor.GOLD + " | " + amountOfTimeTagged);
                    } else {
                        player.setPlayerListName(prefix + player.getName() + ChatColor.GOLD + " | " + amountOfTimeTagged);
                    }
                }
            }
        });
    }
}
