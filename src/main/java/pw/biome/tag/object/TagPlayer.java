package pw.biome.tag.object;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import pro.husk.mysql.MySQL;
import pro.husk.sqlannotations.AnnotatedSQLMember;
import pro.husk.sqlannotations.SinkProcessor;
import pro.husk.sqlannotations.annotations.DatabaseInfo;
import pro.husk.sqlannotations.annotations.DatabaseValue;
import pro.husk.sqlannotations.annotations.UniqueKey;
import pw.biome.tag.database.DatabaseHelper;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@DatabaseInfo(database = "biometag", table = "tag-data")
public class TagPlayer implements AnnotatedSQLMember {

    private static final ConcurrentHashMap<UUID, TagPlayer> tagPlayerMap = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private static boolean globalHintCooldownActive;

    @Getter
    @UniqueKey("uuid")
    private final UUID uuid;

    @Getter
    @Setter
    @DatabaseValue("tagger")
    private UUID tagger;

    @Getter
    @Setter
    @DatabaseValue("username")
    private String username;

    @Getter
    @Setter
    @DatabaseValue("times-tagged")
    private int timesTagged;

    @Setter
    @DatabaseValue("total-time-tagged")
    private int amountOfTimeTagged;

    @Getter
    private final Timer timer;

    @Getter
    private boolean isTagged;

    @Getter
    private boolean loading = true;

    /**
     * Constructor to be used to create TagPlayer objects unless failed to load from database
     *
     * @param uuid of player
     */
    private TagPlayer(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;
        this.timer = new Timer();

        SinkProcessor sinkProcessor = new SinkProcessor(this, () -> {
            tagPlayerMap.put(uuid, this);
            loading = false;
        });
    }

    /**
     * Method to change players tagged flag
     *
     * @param isTagged new tagged value
     */
    public void setTagged(boolean isTagged) {
        this.isTagged = isTagged;

        if (isTagged) {
            timesTagged++;

            timer.start();

            Bukkit.broadcastMessage(ChatColor.YELLOW + "Look out, " + username + " is now " + ChatColor.RED + "IT!");
        } else {
            stopTimer();
        }

        globalHintCooldownActive = false;
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

    public int getAmountOfTimeTagged() {
        if (isTagged) {
            return (int) (System.currentTimeMillis() - timer.getTimeBegin()) / 1000;
        }

        return this.amountOfTimeTagged;
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
     * Method to get a TagPlayer from UUID
     *
     * @param uuid to search
     * @return TagPlayer (if present)
     */
    public static TagPlayer getFromUUID(UUID uuid) {
        return tagPlayerMap.get(uuid);
    }

    public static TagPlayer getOrCreate(UUID uuid, String username) {
        TagPlayer tagPlayer = getFromUUID(uuid);

        if (tagPlayer == null) tagPlayer = new TagPlayer(uuid, username);

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

    @Override
    public MySQL getMySQL() {
        return DatabaseHelper.getMysql();
    }
}
