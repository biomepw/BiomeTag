package pw.biome.tag.object;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import pw.biome.tag.Tag;
import pw.biome.tag.database.DatabaseHelper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TagPlayer {

    private static final ConcurrentHashMap<UUID, TagPlayer> tagPlayerMap = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private static boolean globalHintCooldownActive;

    @Getter
    private final UUID uuid;

    @Getter
    @Setter
    private UUID tagger;

    @Getter
    @Setter
    private String username;

    @Getter
    @Setter
    private int timesTagged;

    @Getter
    @Setter
    private int amountOfTimeTagged;

    @Getter
    private final Timer timer;

    @Getter
    private boolean isTagged;

    @Getter
    private boolean failedToLoad;

    @Getter
    private boolean dataDoesntExist;

    @Getter
    private int taskId;

    /**
     * Constructor to be used to create TagPlayer objects unless failed to load from database
     *
     * @param uuid of player
     */
    private TagPlayer(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.timer = new Timer();

        tagPlayerMap.put(uuid, this);
    }

    /**
     * Method to change players tagged flag
     *
     * @param isTagged new tagged value
     * @return a CompletableFuture of the database save task
     */
    public CompletableFuture<Void> setTagged(boolean isTagged) {
        this.isTagged = isTagged;

        if (isTagged) {
            timesTagged++;

            timer.start();

            Bukkit.broadcastMessage(ChatColor.YELLOW + "Look out, " + username + " is now " + ChatColor.RED + "IT!");

            taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(Tag.getInstance(), this::recurringItemCheckTask, 60 * 20, 60 * 20);
        } else {
            stopTimer();
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = 0;
        }

        globalHintCooldownActive = false;
        CompletableFuture<Void> saveToDatabaseFuture = saveToDatabase();
        Tag.getInstance().updateScoreboards();

        return saveToDatabaseFuture;
    }

    /**
     * Method to start timer
     */
    public void startTimer() {
        timer.start();
    }

    /**
     * Method to stop timer
     */
    public void stopTimer() {
        timer.stop();
        addTimeToAmountOfTimeTagged(timer.getTimeSeconds());
    }

    /**
     * Method to flag data to be scheduled to retry loading
     *
     * @param failedToLoad new failed to load value
     */
    public void setFailedToLoad(boolean failedToLoad) {
        this.failedToLoad = failedToLoad;

        // If data failed to load, schedule to load again
        if (failedToLoad) Bukkit.getScheduler().runTaskLater(Tag.getInstance(), this::updateDataFromDatabase, 5 * 20);
    }

    /**
     * Method to check if the player is AFK
     *
     * @return whether or not the player is AFK
     */
    public boolean isAFK() {
        Player player = Bukkit.getPlayer(uuid);

        if (player == null) return false;
        Team team = player.getScoreboard().getTeam("hc_afk");
        if (team == null) return false;

        return team.hasEntry(player.getName());
    }

    /**
     * Method to calculate the new amountOfTimeTagged
     *
     * @param seconds to add to amountOfTimeTagged
     */
    public void addTimeToAmountOfTimeTagged(int seconds) {
        amountOfTimeTagged += seconds;
    }

    /**
     * Method to update data from database, utilised in /tag sync
     */
    public void updateDataFromDatabase() {
        String query = "SELECT * FROM `tag-data` WHERE `uuid` LIKE '" + uuid.toString() + "';";

        if (isTagged) return; // don't update while tagged as stats will still increment

        CompletableFuture.runAsync(() -> {
            try {
                DatabaseHelper.getMysql().query(query, resultSet -> {
                    while (resultSet.next()) {
                        setTimesTagged(resultSet.getInt("times-tagged"));
                        setAmountOfTimeTagged(resultSet.getInt("total-time-tagged"));
                        setTagger(UUID.fromString(resultSet.getString("tagger")));
                        int tagged = resultSet.getInt("tagged");
                        boolean isTagged = false;

                        if (tagged == 1) isTagged = true;

                        this.isTagged = isTagged; // set directly so we avoid conflict
                    }
                    if (failedToLoad) {
                        failedToLoad = false;
                    }
                });
            } catch (SQLException throwables) {
                System.out.println("An error occurred while fetching user's data from the database. " +
                        "We will try recover. Here's the stack trace");

                throwables.printStackTrace();

                setFailedToLoad(true);
            }
        });
    }

    /**
     * Method used to save to database
     *
     * @return a CompletableFuture of the progress
     */
    public CompletableFuture<Void> saveToDatabase() {
        return CompletableFuture.runAsync(() -> {
            int tagged = isTagged() ? 1 : 0;

            String update = "UPDATE `tag-data` SET `username`='" + getUsername() + "',`times-tagged`='" +
                    getTimesTagged() + "',`total-time-tagged`='" + getAmountOfTimeTagged() +
                    "',`tagged`=" + tagged + ", `tagger`='" + getTagger().toString() + "' WHERE `uuid` LIKE '" + getUuid().toString() + "';";

            DatabaseHelper.getMysql().updateAsync(update).exceptionally(exception -> {
                exception.printStackTrace();
                return 0;
            });
        });
    }

    /**
     * Method to get a TagPlayer from UUID
     *
     * @param uuid to search
     * @return TagPlayer (if present)
     */
    public static TagPlayer getFromUUID(UUID uuid) {
        return tagPlayerMap.get(uuid);
    }

    /**
     * Method to try and load the player data from database
     *
     * @param uuid to try load from database
     * @return TagPlayer that was loaded
     */
    public static TagPlayer tryLoadFromDatabaseOrCreate(UUID uuid, String username) {
        TagPlayer tagPlayer = tryLoadFromDatabase(uuid, username);

        if (tagPlayer.dataDoesntExist) {
            // Set the values we do know
            tagPlayer.setUsername(username);
            tagPlayer.setTagger(UUID.fromString("00000000-0000-0000-0000-000000000000"));

            String insert = "INSERT INTO `tag-data` (`uuid`, `username`, `times-tagged`, `total-time-tagged`, `tagged`, `tagger`)" +
                    " VALUES ('" + uuid.toString() + "', '" + tagPlayer.getUsername() + "', '0', '0', '0', '" + tagPlayer.getTagger() + "');";
            DatabaseHelper.getMysql().updateAsync(insert);

            // Data exists now!
            tagPlayer.dataDoesntExist = false;
        }

        return tagPlayer;
    }

    /**
     * Try and load TagPlayer from database
     *
     * @param uuid of player to try load
     * @return TagPlayer or null if not found
     */
    public static TagPlayer tryLoadFromDatabase(UUID uuid, String username) {
        TagPlayer tagPlayer;

        if (tagPlayerMap.get(uuid) == null) {
            tagPlayer = new TagPlayer(uuid, username);
        } else {
            tagPlayer = tagPlayerMap.get(uuid);
        }

        String query = "SELECT * FROM `tag-data` WHERE `uuid` LIKE '" + uuid.toString() + "';";

        try (ResultSet resultSet = DatabaseHelper.getMysql().query(query)) {
            if (!resultSet.next()) {
                tagPlayer.dataDoesntExist = true;
                return tagPlayer;
            }

            tagPlayer.setUsername(resultSet.getString("username"));
            tagPlayer.setTimesTagged(resultSet.getInt("times-tagged"));
            tagPlayer.setAmountOfTimeTagged(resultSet.getInt("total-time-tagged"));
            tagPlayer.setTagger(UUID.fromString(resultSet.getString("tagger")));
            int tagged = resultSet.getInt("tagged");
            boolean isTagged = false;

            if (tagged == 1) isTagged = true;

            tagPlayer.isTagged = isTagged; // set directly so we avoid conflict
        } catch (SQLException throwables) {
            tagPlayer.setFailedToLoad(true);
            throwables.printStackTrace();
        }

        return tagPlayer;
    }

    /**
     * Method to get currently tagged player (if loaded in memory) alternatively use DatabaseHelper.getCurrentTaggedPlayer
     *
     * @return currently tagged player object
     */
    public static TagPlayer getTaggedPlayer() {
        for (TagPlayer tagPlayer : tagPlayerMap.values()) {
            if (tagPlayer.isTagged) return tagPlayer;
        }
        return null; // shouldn't ever be null
    }

    /**
     * Method to return TagPlayer from name
     *
     * @param name of TagPlayer
     * @return TagPlayer (if found)
     */
    public static TagPlayer getFromName(String name) {
        for (TagPlayer tagPlayer : tagPlayerMap.values()) {
            if (tagPlayer.getUsername().equals(name)) return tagPlayer;
        }
        return null;
    }

    /**
     * Method used to dump all TagPlayer data without saving
     */
    public static void dumpAllData() {
        tagPlayerMap.clear();
    }

    /**
     * Recurring method to check if the player that is tagged, still has the tag. If not, check to see who has it and adjust accordingly
     */
    public void recurringItemCheckTask() {
        CompletableFuture.runAsync(() -> {
            if (!isTagged) return;

            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                if (!TagItem.inventoryContains(player.getInventory().getContents())) {
                    setTagged(false).thenRun(() -> {
                        // If there is no other currently tagged player (which there shouldn't be)
                        DatabaseHelper.getCurrentTaggedPlayer().thenAcceptAsync(currentTaggedPlayer -> {
                            if (currentTaggedPlayer == null) {
                                ImmutableList<Player> playerImmutableList = ImmutableList.copyOf(Bukkit.getServer().getOnlinePlayers());
                                for (Player online : playerImmutableList) {
                                    ItemStack[] invContents = online.getInventory().getContents();

                                    if (TagItem.inventoryContains(invContents)) {
                                        setTagged(true);
                                    }
                                }
                            }
                        });
                    });
                }
            }
        });
    }
}
