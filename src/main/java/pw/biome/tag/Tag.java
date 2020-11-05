package pw.biome.tag;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pw.biome.biomechat.BiomeChat;
import pw.biome.biomechat.obj.Corp;
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
    private static boolean gameRunning;

    @Getter
    private int scoreboardTaskId;

    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        DatabaseHelper.setupDatabase();
        TagItem.initiateItem();

        PaperCommandManager manager = new PaperCommandManager(instance);
        manager.registerCommand(new TagCommand());

        getServer().getPluginManager().registerEvents(new TagListener(), this);
    }

    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());
            if (tagPlayer != null) {
                tagPlayer.saveToDatabase();
            }
        }
    }

    public void startGame() {
        if (!gameRunning) {
            DatabaseHelper.syncData().thenRun(() -> {
                BiomeChat biomeChat = BiomeChat.getPlugin();
                biomeChat.registerHook(this);
                biomeChat.stopScoreboardTask();
                this.restartScoreboardTask();

                gameRunning = true;
            });
        }
    }

    public void stopGame() {
        if (gameRunning) {
            DatabaseHelper.syncData().thenRun(() -> {
                this.stopScoreboardTask();
                BiomeChat biomeChat = BiomeChat.getPlugin();
                biomeChat.unregisterHook(this);
                biomeChat.restartScoreboardTask();

                gameRunning = false;
            });
        }
    }

    @Override
    public void restartScoreboardTask() {
        if (scoreboardTaskId == 0) {
            scoreboardTaskId = getServer().getScheduler()
                    .runTaskTimerAsynchronously(this, () -> {
                        for (Player player : getServer().getOnlinePlayers()) {
                            TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());
                            Corp corp = Corp.getCorpForUser(player.getUniqueId());

                            boolean afk = tagPlayer.isAFK();

                            int amountOfTimeTagged = tagPlayer.getAmountOfTimeTagged() / 60;
                            ChatColor prefix = corp.getPrefix();

                            if (tagPlayer.isTagged()) prefix = ChatColor.DARK_RED;

                            if (afk) {
                                player.setPlayerListName(ChatColor.GRAY + player.getName() + ChatColor.GOLD + " | " + amountOfTimeTagged);
                            } else {
                                player.setPlayerListName(prefix + player.getName() + ChatColor.GOLD + " | " + amountOfTimeTagged);
                            }
                        }
                    }, (10 * 20), (10 * 20)).getTaskId();
        }
    }

    @Override
    public void stopScoreboardTask() {
        if (scoreboardTaskId != 0) {
            getServer().getScheduler().cancelTask(scoreboardTaskId);
            scoreboardTaskId = 0;
        }
    }
}
