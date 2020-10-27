package pw.biome.tag.listener;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import pw.biome.biomechat.obj.Corp;
import pw.biome.biomechatrelay.util.ChatUtility;
import pw.biome.tag.Tag;
import pw.biome.tag.object.TagItem;
import pw.biome.tag.object.TagPlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TagListener implements Listener {

    @EventHandler
    public void asyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!Tag.isGameRunning()) return;
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
            Bukkit.getScheduler().runTask(Tag.getInstance(), () -> {
                String joinMessage = ChatColor.YELLOW + "Hey look. " + ChatColor.RED +
                        tagPlayer.getUsername() + ChatColor.YELLOW + " has logged in," +
                        " and they're " + ChatColor.RED + "IT!";
                Bukkit.broadcastMessage(joinMessage);
                ChatUtility.sendToDiscord(joinMessage);
            });
        }
    }

    @EventHandler
    public void saveDataOnLeave(PlayerQuitEvent event) {
        if (!Tag.isGameRunning()) return;
        TagPlayer tagPlayer = TagPlayer.getFromUUID(event.getPlayer().getUniqueId());
        if (!tagPlayer.isFailedToLoad()) {
            tagPlayer.stopTimer();
            tagPlayer.saveToDatabase();
        }

        if (tagPlayer.isTagged()) {
            event.setQuitMessage(ChatColor.YELLOW + "Pffft. " + tagPlayer.getUsername() + " left? What a pussy!");
        }
    }

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        if (!Tag.isGameRunning()) return;
        Player player = event.getPlayer();
        TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());

        if (!tagPlayer.isTagged()) return;

        // Don't let people tag animals!
        if (TagItem.equals(player.getInventory().getItemInMainHand()) ||
                TagItem.equals(player.getInventory().getItemInOffHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void taggedPlayer(EntityDamageByEntityEvent event) {
        if (!Tag.isGameRunning()) return;
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player damaged = (Player) event.getEntity();

            TagPlayer damagerTagPlayer = TagPlayer.getFromUUID(damager.getUniqueId());
            TagPlayer damagedTagPlayer = TagPlayer.getFromUUID(damaged.getUniqueId());

            // If the damager isnt tagged, do nothing
            if (!damagerTagPlayer.isTagged()) return;

            // If the damager's tagger is the person whacked, do nothing -- don't allow tagger to tag their tagger
            if (damagerTagPlayer.getTagger().equals(damaged.getUniqueId())) return;

            // Disable tagging afk players
            if (damagedTagPlayer.isAFK()) {
                damager.sendMessage(ChatColor.RED + "This player is AFK! You can't tag them!");
                return;
            }

            // Move the tag
            damagedTagPlayer.setTagged(true);
            damagerTagPlayer.setTagged(false);

            Corp taggerCorp = Corp.getCorpForUser(damagerTagPlayer.getUuid());

            String tagMessage = ChatColor.GOLD + "Look out! " + taggerCorp.getPrefix() +
                    damagerTagPlayer.getUsername() + ChatColor.GOLD + " just tagged "
                    + ChatColor.RED + damagedTagPlayer.getUsername();

            // Broadcast the message to game + discord
            Bukkit.broadcastMessage(tagMessage);
            ChatUtility.sendToDiscord(tagMessage);

            // Edit inventories
            damager.getInventory().removeItem(TagItem.getTagItem());

            // If their inv is full, drop it on ground
            if (damaged.getInventory().addItem(TagItem.getTagItem()).size() != 0) {
                damaged.getLocation().getWorld().dropItemNaturally(damaged.getLocation(), TagItem.getTagItem());
            }
        }
    }

    @EventHandler
    public void playerDeath(PlayerDeathEvent event) {
        if (!Tag.isGameRunning()) return;
        Player player = event.getEntity();
        TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());

        // Remove tag from the dropped items
        if (tagPlayer.isTagged()) {
            event.getDrops().remove(TagItem.getTagItem());
        }
    }

    @EventHandler
    public void playerRespawn(PlayerRespawnEvent event) {
        if (!Tag.isGameRunning()) return;
        Player player = event.getPlayer();
        TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());

        // Add tag back to player when they respawn
        if (tagPlayer.isTagged()) {
            player.getInventory().addItem(TagItem.getTagItem());
        }
    }

    @EventHandler
    public void itemPickupEvent(EntityPickupItemEvent event) {
        if (!Tag.isGameRunning()) return;
        ItemStack itemStack = event.getItem().getItemStack();

        // Disable picking up tag item across all entities
        if (TagItem.equals(itemStack)) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                TagPlayer tagPlayer = TagPlayer.getFromUUID(player.getUniqueId());

                // If the player is tagged, let them pick it up
                if (tagPlayer.isTagged()) event.setCancelled(false);
                return;
            }

            event.setCancelled(true);
        }
    }
}
