package pw.biome.tag.database;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import pro.husk.mysql.MySQL;
import pw.biome.tag.Tag;
import pw.biome.tag.object.TagPlayer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public final class DatabaseHelper {

    @Getter
    private static MySQL mysql;

    /**
     * Method to set up database
     */
    public static void setupDatabase() {
        FileConfiguration config = Tag.getInstance().getConfig();

        String hostname = config.getString("mysql.hostname");
        String port = config.getString("mysql.port");
        String database = config.getString("mysql.database");
        boolean useSSL = config.getBoolean("mysql.useSSL");
        String username = config.getString("mysql.username");
        String password = config.getString("mysql.password");
        String jdbcUrl = "jdbc:mysql://" + hostname + ":" + port + "/" + database + "?useSSL=" + useSSL;

        mysql = new MySQL(jdbcUrl, username, password);
    }

    /**
     * Method to get current tagged player name from database directly
     *
     * @return CompletableFuture of the current tagged player's name
     */
    public static CompletableFuture<String> getCurrentTaggedPlayer() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResultSet resultSet = mysql.query("SELECT `username` FROM `tag-data` WHERE `tagged` = 1;");

                if (resultSet.next()) {
                    return resultSet.getString("username");
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return "";
        });
    }

    /**
     * Method to forcibly remove tagged players tagged status and add it to the given player (ignores local memory)
     *
     * @param tagPlayer to set tagged
     */
    public static void removeTaggedPlayerAndAddTo(TagPlayer tagPlayer) {
        // Remove tag from current player
        TagPlayer currentTagged = TagPlayer.getTaggedPlayer();

        if (currentTagged != null) currentTagged.setTagged(false);

        String update = "UPDATE `tag-data` SET `tagged` = 0 WHERE `tagged`= '1';";
        mysql.updateAsync(update).exceptionally(exception -> {
            exception.printStackTrace();
            return 0;
        });

        // Add current tag to player
        String tagUpdate = "UPDATE `tag-data` SET `tagged` = 1 WHERE uuid LIKE '" + tagPlayer.getUuid().toString() + "';";
        mysql.updateAsync(tagUpdate).exceptionally(exception -> {
            exception.printStackTrace();
            return 0;
        });

        tagPlayer.setTagged(true);
    }

    /**
     * Method to sync local cache with database
     *
     * @return CompletableFuture of progress
     */
    public static CompletableFuture<Void> syncData() {
        TagPlayer currentTagged = TagPlayer.getTaggedPlayer();

        if (currentTagged != null) {
            currentTagged.stopTimer();
            currentTagged.saveToDatabase();
        }

        TagPlayer.dumpAllData();

        return CompletableFuture.runAsync(() -> {
            ImmutableList<Player> playerImmutableList = ImmutableList.copyOf(Bukkit.getServer().getOnlinePlayers());
            playerImmutableList.forEach(player -> TagPlayer.tryLoadFromDatabaseOrCreate(player.getUniqueId(), player.getName()));

            TagPlayer newCurrentTaggedPlayer = TagPlayer.getTaggedPlayer();

            if (newCurrentTaggedPlayer == null) return;

            newCurrentTaggedPlayer.getTimer().start();
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}
