package pw.biome.tag.object;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TagItem {

    @Getter
    private static ItemStack tagItem;

    /**
     * Method to initialise the tagItem
     */
    public static void initiateItem() {
        tagItem = new ItemStack(Material.NAME_TAG, 1);
        ItemMeta itemMeta = tagItem.getItemMeta();

        itemMeta.setDisplayName(ChatColor.GOLD + "Tag! You're it!");

        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.GREEN + "Welcome to the Tag game! The player who holds this item is IT!");
        lore.add(ChatColor.GREEN + "It is now your goal to go tag another player! (Punch them with the tag!)");
        lore.add(ChatColor.GREEN + "The winner is whoever holds the tag for the least amount of time!");

        itemMeta.setLore(lore);

        tagItem.setItemMeta(itemMeta);
    }

    /**
     * Quick and easy utility method to compare an itemstack to the tag item
     *
     * @param given itemstack to compare
     * @return whether or not the items are equal
     */
    public static boolean equals(ItemStack given) {
        if (given == null) return false;

        ItemMeta givenMeta = given.getItemMeta();
        ItemMeta tagMeta = tagItem.getItemMeta();

        if (givenMeta == null) return false;

        if (given.getType().equals(tagItem.getType()) && givenMeta.getDisplayName().equals(tagMeta.getDisplayName())) {
            return givenMeta.getLore().equals(tagMeta.getLore());
        }
        return false;
    }

    /**
     * Quick and easy utility method to compare contents and check if the tag item is inside
     *
     * @param contents to search
     * @return whether the inventory contents contain the tag item
     */
    public static boolean inventoryContains(ItemStack[] contents) {
        for (ItemStack item : contents) {
            if (equals(item)) return true;
        }
        return false;
    }
}
