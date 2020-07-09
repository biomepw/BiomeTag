package pw.biome.tag;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pw.biome.biomechat.BiomeChat;
import pw.biome.biomechat.obj.PlayerCache;
import pw.biome.biomechat.obj.ScoreboardHook;
import pw.biome.tag.commands.TagCommand;
import pw.biome.tag.database.DatabaseHelper;
import pw.biome.tag.listener.TagListener;
import pw.biome.tag.object.TagItem;
import pw.biome.tag.object.TagPlayer;

public class Tag extends JavaPlugin implements ScoreboardHook {

    @Getter
    private static Tag instance;

    @Getter
    private int scoreboardTaskId;

    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        DatabaseHelper.setupDatabase();
        TagItem.initiateItem();

        getCommand("tag").setExecutor(new TagCommand());
        getServer().getPluginManager().registerEvents(new TagListener(), this);

        BiomeChat biomeChat = BiomeChat.getPlugin();
        biomeChat.registerHook(this);
        biomeChat.stopScoreboardTask();

        scoreboardTaskId = getServer().getScheduler()
                .runTaskTimerAsynchronously(this, this::restartScoreboardTask, (10 * 20), (10 * 20)).getTaskId();
    }

    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());
            tagPlayer.saveToDatabase();
        }
    }

    @Override
    public void restartScoreboardTask() {
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
    }

    @Override
    public void stopScoreboardTask() {
        if (scoreboardTaskId != 0) getServer().getScheduler().cancelTask(scoreboardTaskId);
    }
}
