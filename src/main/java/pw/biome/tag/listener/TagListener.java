package pw.biome.tag.listener;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import pw.biome.tag.Tag;
import pw.biome.tag.database.DatabaseHelper;
import pw.biome.tag.object.TagItem;
import pw.biome.tag.object.TagPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TagListener implements Listener {

    @EventHandler
    public void asyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();

        TagPlayer tagPlayer = TagPlayer.getFromUUID(uuid);
        String username = event.getName();

        if (tagPlayer == null) {
            CompletableFuture<TagPlayer> tagPlayerCompletableFuture = CompletableFuture.supplyAsync(() ->
                    TagPlayer.tryLoadFromDatabaseOrCreate(uuid, username)).exceptionally(exception -> {
                exception.printStackTrace();
                return null;
            });

            tagPlayerCompletableFuture.thenAcceptAsync(this::handleJoin);
        } else {
            handleJoin(tagPlayer);
        }
    }

    private void handleJoin(TagPlayer tagPlayer) {
        if (tagPlayer.isTagged()) {
            tagPlayer.startTimer();
            Bukkit.getScheduler().runTask(Tag.getInstance(), () ->
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Hey look. " + tagPlayer.getUsername() + " has logged in," +
                            " and they're " + ChatColor.RED + "IT! Go kill them!"));
        }
    }

    @EventHandler
    public void saveDataOnLeave(PlayerQuitEvent event) {
        TagPlayer tagPlayer = TagPlayer.getFromUUID(event.getPlayer().getUniqueId());
        if (!tagPlayer.isFailedToLoad()) {
            tagPlayer.stopTimer();
            tagPlayer.saveToDatabase();
        }

        if (tagPlayer.isTagged()) {
            event.setQuitMessage(ChatColor.YELLOW + "Pffft. " + tagPlayer.getUsername() + " left? What a pussy");
        }
    }

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());

        if (!tagPlayer.isTagged()) return;

        if (TagItem.equals(player.getInventory().getItemInMainHand()) ||
                TagItem.equals(player.getInventory().getItemInOffHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void playerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());

        if (tagPlayer.isTagged()) {
            tagPlayer.setTagged(false);
        }
    }

    @EventHandler
    public void itemPickupEvent(EntityPickupItemEvent event) {
        LivingEntity livingEntity = event.getEntity();

        if (livingEntity instanceof Player) {
            ItemStack itemStack = event.getItem().getItemStack();

            if (TagItem.equals(itemStack)) {
                Player player = (Player) livingEntity;
                TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());

                DatabaseHelper.removeTaggedPlayerAndAddTo(tagPlayer);
            }
        }
    }
}
