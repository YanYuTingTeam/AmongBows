package com.aermini.amongbows.utils;

import com.aermini.amongbows.game.TeamColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameItems {

    public static final String BOW = "bow";
    public static final String GUESS = "guess";
    public static final String HEAL = "heal";
    public static final String GAPPLE = "gapple";
    public static final String CLUEBOOK = "cluebook";
    public static final String SCROLL = "scroll";
    public static final String AMULET = "amulet";
    public static final String SKILLPACK = "skillpack";
    public static final String ARROWPACK = "arrowpack";

    private static final Map<String, String> NAME_TO_OID = new HashMap<>();
    private static final Map<String, Integer> LIMITS = new HashMap<>();

    static {
        LIMITS.put(BOW, 1);
        LIMITS.put(GUESS, 1);
        LIMITS.put(GAPPLE, 3);
        LIMITS.put(CLUEBOOK, 1);
        LIMITS.put(SCROLL, 1);
        LIMITS.put(AMULET, 1);
    }

    public static ItemStack bow(String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(msg.t(name));
        meta.setLore(loreList(lore));
        item.setItemMeta(meta);
        NAME_TO_OID.put(meta.getDisplayName(), BOW);
        return item;
    }

    public static ItemStack guess() {
        ItemStack item = named(Material.COMPASS, "&b&l猜测指南针",
            "&7右键打开猜测UI", "&7选择目标玩家并猜测其真实队伍");
        NAME_TO_OID.put(item.getItemMeta().getDisplayName(), GUESS);
        return item;
    }

    public static ItemStack heal(int amount) {
        ItemStack item = named(Material.POTION, "&c&l治疗药水",
            "&7左/右键点击玩家: 为其恢复5HP, 自身扣除2HP",
            "&7丢弃: 为自身恢复3HP");
        item.setAmount(Math.max(1, amount));
        NAME_TO_OID.put(item.getItemMeta().getDisplayName(), HEAL);
        return item;
    }

    public static ItemStack gapple() {
        ItemStack item = named(Material.GOLDEN_APPLE, "&6&l金苹果",
            "&7永久+2HP, 最多叠加至+5HP");
        NAME_TO_OID.put(item.getItemMeta().getDisplayName(), GAPPLE);
        return item;
    }

    public static ItemStack cluebook() {
        ItemStack item = named(Material.BOOK, "&e&l线索书",
            "&7右键获得1~2条随机线索");
        NAME_TO_OID.put(item.getItemMeta().getDisplayName(), CLUEBOOK);
        return item;
    }

    public static ItemStack scroll() {
        ItemStack item = named(Material.PAPER, "&b&l传送卷轴",
            "&7右键随机传送到安全落脚点");
        NAME_TO_OID.put(item.getItemMeta().getDisplayName(), SCROLL);
        return item;
    }

    public static ItemStack amulet() {
        ItemStack item = named(Material.EMERALD, "&a&l护身符",
            "&7抵挡一次致命伤害, 触发后获得2秒无敌");
        NAME_TO_OID.put(item.getItemMeta().getDisplayName(), AMULET);
        return item;
    }

    public static ItemStack skillpack() {
        ItemStack item = named(Material.EXP_BOTTLE, "&d&l技能点包",
            "&7拾取: 个人技能点+1, 队伍技能点+1");
        NAME_TO_OID.put(item.getItemMeta().getDisplayName(), SKILLPACK);
        return item;
    }

    public static ItemStack arrowpack() {
        ItemStack item = named(Material.ARROW, "&f&l箭矢包",
            "&7拾取: 获得3支箭");
        NAME_TO_OID.put(item.getItemMeta().getDisplayName(), ARROWPACK);
        return item;
    }

    public static ItemStack jersey(TeamColor color) {
        String[] names = {"&c红队队服", "&b蓝队队服", "&a绿队队服", "&e黄队队服"};
        int idx = color.ordinal();
        ItemStack item = named(Material.LEATHER_CHESTPLATE, names[idx],
            "&7手持右键穿戴整套队服", "&7队服颜色不代表真实队伍");
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color.leather);
        item.setItemMeta(meta);
        NAME_TO_OID.put(item.getItemMeta().getDisplayName(), "jersey_" + color.id);
        return item;
    }

    public static ItemStack jerseyPiece(TeamColor color, Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color.leather);
        meta.setDisplayName(jerseyName(color));
        meta.setLore(loreList(java.util.Collections.singletonList("&7队服颜色不代表真实队伍")));
        item.setItemMeta(meta);
        return item;
    }

    public static String jerseyName(TeamColor color) {
        String[] names = {"&c红队队服", "&b蓝队队服", "&a绿队队服", "&e黄队队服"};
        return msg.t(names[color.ordinal()]);
    }

    public static String oidOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;
        return NAME_TO_OID.get(meta.getDisplayName());
    }

    public static int limitOf(String oid) {
        Integer limit = LIMITS.get(oid);
        return limit == null ? Integer.MAX_VALUE : limit;
    }

    public static boolean isJerseyOid(String oid) {
        return oid != null && oid.startsWith("jersey_");
    }

    public static TeamColor jerseyColor(String oid) {
        return isJerseyOid(oid) ? TeamColor.byId(oid.substring("jersey_".length())) : null;
    }

    public static int countOf(Player p, String oid) {
        int n = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (oidOf(item) != null && oidOf(item).equals(oid)) n += item.getAmount();
        }
        return n;
    }

    public static int countArrows(Player p) {
        int n = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ARROW) n += item.getAmount();
        }
        return n;
    }

    public static void addArrows(Player p, int n) {
        Map<Integer, ItemStack> overflow = p.getInventory().addItem(new ItemStack(Material.ARROW, n));
        for (ItemStack rest : overflow.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), rest);
        }
    }

    public static void removeArrows(Player p, int n) {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length && n > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != Material.ARROW) continue;
            int take = Math.min(n, item.getAmount());
            n -= take;
            if (take >= item.getAmount()) {
                p.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - take);
            }
        }
    }

    public static boolean consumeOne(Player p, String oid) {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            String o = oidOf(item);
            if (oid.equals(o)) {
                if (item.getAmount() <= 1) {
                    p.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - 1);
                }
                p.updateInventory();
                return true;
            }
        }
        return false;
    }

    private static ItemStack named(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(msg.t(name));
        meta.setLore(loreList(java.util.Arrays.asList(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private static List<String> loreList(List<String> lore) {
        List<String> list = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                list.add(msg.t(line));
            }
        }
        return list;
    }
}
