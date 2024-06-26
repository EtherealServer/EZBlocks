package me.clip.ezblocks.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import me.clip.ezblocks.tasks.LoadTask;
import me.clip.ezblocks.tasks.PlayerSaveTask;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BreakHandler implements Listener {

    EZBlocks plugin;

    public static HashMap<String, Integer> breaks = new HashMap<String, Integer>();

    public BreakHandler(EZBlocks i) {
        plugin = i;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        String uuid = e.getPlayer().getUniqueId().toString();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new LoadTask(plugin, uuid));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        String uuid = e.getPlayer().getUniqueId().toString();
        if (breaks.containsKey(uuid)) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new PlayerSaveTask(plugin, uuid, breaks.get(uuid)));
            breaks.remove(uuid);
        }
    }

    private boolean isAllowedBlock(Material m) {
        if (EZBlocks.options.getBlacklistedBlocks() == null || EZBlocks.options.getBlacklistedBlocks().isEmpty()) {
            return true;
        }

        if (EZBlocks.options.blacklistIsWhitelist()) {
            if (!EZBlocks.options.getBlacklistedBlocks().contains(m.toString())) {
                return false;
            }
        } else {
            if (EZBlocks.options.getBlacklistedBlocks().contains(m.toString())) {
                return false;
            }
        }

        return true;
    }

    private boolean isTool(ItemStack i) {
        return EZBlocks.options.getTrackedTools() != null 
            && (EZBlocks.options.getTrackedTools().contains(i.getType().name()) 
            || isNetheriteTool(i.getType()));
    }

    private boolean isNetheriteTool(Material material) {
        return material == Material.NETHERITE_PICKAXE ||
               material == Material.NETHERITE_AXE ||
               material == Material.NETHERITE_SHOVEL;
    }

    private String getName(ItemStack i) {
        String type = "";
        switch (i.getType().name()) {
            case "WOOD_PICKAXE":
            case "WOODEN_PICKAXE":
                type = "Wood Pickaxe";
                break;
            case "STONE_PICKAXE":
                type = "Stone Pickaxe";
                break;
            case "IRON_PICKAXE":
                type = "Iron Pickaxe";
                break;
            case "GOLD_PICKAXE":
                type = "Golden Pickaxe";
                break;
            case "DIAMOND_PICKAXE":
                type = "Diamond Pickaxe";
                break;
            case "NETHERITE_PICKAXE":
                type = "Netherite Pickaxe";
                break;
            case "WOOD_AXE":
            case "WOODEN_AXE":
                type = "Wood Axe";
                break;
            case "STONE_AXE":
                type = "Stone Axe";
                break;
            case "IRON_AXE":
                type = "Iron Axe";
                break;
            case "GOLD_AXE":
                type = "Golden Axe";
                break;
            case "DIAMOND_AXE":
                type = "Diamond Axe";
                break;
            case "NETHERITE_AXE":
                type = "Netherite Axe";
                break;
            case "WOOD_SPADE":
            case "WOODEN_SHOVEL":
                type = "Wood Spade";
                break;
            case "STONE_SPADE":
            case "STONE_SHOVEL":
                type = "Stone Spade";
                break;
            case "IRON_SPADE":
            case "IRON_SHOVEL":
                type = "Iron Spade";
                break;
            case "GOLD_SPADE":
            case "GOLDEN_SHOVEL":
                type = "Golden Spade";
                break;
            case "DIAMOND_SPADE":
            case "DIAMOND_SHOVEL":
                type = "Diamond Spade";
                break;
            case "NETHERITE_SHOVEL":
                type = "Netherite Shovel";
                break;
        }

        if (type.equals("")) {
            return i.getType().name();
        }

        return type;
    }

    @SuppressWarnings("deprecation")
    public boolean check(Player p, Block b) {
        if (!isAllowedBlock(b.getType())) {
            return false;
        }

        ItemStack i = p.getItemInHand();

        if (i == null) {
            return false;
        }

        if (!isTool(i)) {
            return false;
        }

        if (EZBlocks.options.survivalOnly() && !p.getGameMode().equals(GameMode.SURVIVAL)) {
            return false;
        }

        if (!EZBlocks.options.getEnabledWorlds().contains(p.getWorld().getName())
                && !EZBlocks.options.getEnabledWorlds().contains("all")) {
            return false;
        }

        if (EZBlocks.options.onlyBelowY()
                && b.getLocation().getBlockY() > EZBlocks.options.getBelowYCoord()) {
            return false;
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    public void handleBlockBreakEvent(final Player p, final Block block) {
        ItemStack i = p.getItemInHand();
        String uuid = p.getUniqueId().toString();
        int b;

        if (!breaks.containsKey(uuid)) {
            if (plugin.playerconfig.hasData(uuid)) {
                b = plugin.playerconfig.getBlocksBroken(uuid) + 1;
            } else {
                b = 1;
            }
        } else {
            b = breaks.get(uuid) + 1;
        }

        breaks.put(uuid, b);
        plugin.rewards.giveReward(p, b);
        plugin.rewards.giveIntervalReward(p, b);

        if (EZBlocks.options.pickaxeNeverBreaks()) {
            i.setDurability((short) 0);
        }

        if (EZBlocks.options.usePickCounter() && p.hasPermission("ezblocks.pickaxecounter")) {
            handlePickCounter(p, i);
        }
    }

    private void handlePickCounter(Player p, ItemStack i) {
        String format = ChatColor.translateAlternateColorCodes('&', EZBlocks.options.getPickCounterFormat());
        int one = format.indexOf('%');
        int two = format.lastIndexOf('%');
        String first = format.substring(0, one);
        String second = format.substring(two + 1);

        ItemMeta meta = i.getItemMeta();

        if (EZBlocks.options.usePickCounterDisplayName()) {
            int breaks = 1;

            if (i.hasItemMeta() && i.getItemMeta().hasDisplayName()) {
                String displayName = i.getItemMeta().getDisplayName();

                if (displayName.startsWith(first) && displayName.endsWith(second)) {
                    String f = displayName.replace(first, "");
                    f = f.replace(second, "").trim();
                    int amt = getInt(f);
                    breaks = amt + 1;
                    meta.setDisplayName(format.replace("%blocks%", String.valueOf(breaks)));
                    i.setItemMeta(meta);
                    plugin.rewards.givePickaxeReward(p, breaks);
                    plugin.rewards.givePickaxeIntervalReward(p, breaks);
                } else if (displayName.contains(" " + first) && displayName.endsWith(second)) {
                    int split = displayName.indexOf(first, 0);
                    String name = displayName.substring(0, split);
                    String f = displayName.substring(split);
                    f = f.replace(first, "");
                    f = f.replace(second, "").trim();

                    int amt = getInt(f);
                    breaks = amt + 1;
                    meta.setDisplayName(name + format.replace("%blocks%", String.valueOf(breaks)));
                    i.setItemMeta(meta);
                    plugin.rewards.givePickaxeReward(p, breaks);
                    plugin.rewards.givePickaxeIntervalReward(p, breaks);
                } else {
                    meta.setDisplayName(displayName + " " + format.replace("%blocks%", "1"));
                    i.setItemMeta(meta);
                    plugin.rewards.givePickaxeReward(p, 1);
                    plugin.rewards.givePickaxeIntervalReward(p, 1);
                }
            } else {
                String type = getName(i);
                meta.setDisplayName(type + " " + format.replace("%blocks%", "1"));
                i.setItemMeta(meta);
                plugin.rewards.givePickaxeReward(p, 1);
                plugin.rewards.givePickaxeIntervalReward(p, 1);
            }
        } else {
            if (i.hasItemMeta() && i.getItemMeta().hasLore()) {
                int breaks = 0;
                boolean contains = false;
                List<String> lore = meta.getLore();
                List<String> newLore = new ArrayList<String>();

                for (String line : lore) {
                    if (line.startsWith(first) && line.endsWith(second)) {
                        contains = true;
                        String amount = line.replace(first, "").replace(second, "");

                        breaks = getInt(amount);
                        newLore.add(format.replace("%blocks%", String.valueOf(breaks + 1)));
                    } else {
                        newLore.add(line);
                    }
                }

                if (!contains) {
                    newLore.add(format.replace("%blocks%", "1"));
                }

                meta.setLore(newLore);
                i.setItemMeta(meta);
                plugin.rewards.givePickaxeReward(p, breaks);
                plugin.rewards.givePickaxeIntervalReward(p, breaks);
            } else {
                List<String> lore = new ArrayList<String>();
                lore.add(format.replace("%blocks%", "1"));
                meta.setLore(lore);
                i.setItemMeta(meta);
                plugin.rewards.givePickaxeReward(p, 1);
                plugin.rewards.givePickaxeIntervalReward(p, 1);
            }
        }
    }

    public int getInt(String s) {
        try {
            int i = Integer.parseInt(s);
            return i;
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
