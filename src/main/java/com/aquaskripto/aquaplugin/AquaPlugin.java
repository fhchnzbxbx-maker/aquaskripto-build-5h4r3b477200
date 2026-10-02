package com.aquaskripto.aquaplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class AquaPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private NamespacedKey itemKey;

    @Override
    public void onEnable() {
        itemKey = new NamespacedKey(this, "custom_item");
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Custom itemy wlaczone! GUI: Shift + F.");
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder()
                    instanceof CustomMenu) {
                player.closeInventory();
            }
        }
        cooldowns.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().isSneaking()) {
            return;
        }

        event.setCancelled(true);
        openMenu(event.getPlayer());
    }

    private void openMenu(Player player) {
        CustomMenu menu = new CustomMenu();
        Inventory inventory = menu.getInventory();

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(10, createItem("storm"));
        inventory.setItem(12, createItem("dash"));
        inventory.setItem(14, createItem("heart"));
        inventory.setItem(16, createItem("miner"));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.7f, 1.3f);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CustomMenu)) {
            return;
        }

        // Blokuje także shift-click, klawisze cyfr oraz podwójne kliknięcia.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!event.isLeftClick() && !event.isRightClick()) {
            return;
        }

        String id = switch (event.getRawSlot()) {
            case 10 -> "storm";
            case 12 -> "dash";
            case 14 -> "heart";
            case 16 -> "miner";
            default -> null;
        };

        if (id == null) {
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "Zwolnij jedno miejsce w ekwipunku!");
            return;
        }

        player.getInventory().addItem(createItem(id));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        player.sendMessage(ChatColor.AQUA + "Otrzymujesz autorski przedmiot! Moc: PPM.");
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CustomMenu) {
            event.setCancelled(true);
        }
    }

    private ItemStack createItem(String id) {
        Material material;
        String name;
        String description;
        int cooldown;

        switch (id) {
            case "storm" -> {
                material = Material.DIAMOND_SWORD;
                name = ChatColor.AQUA + "⚡ Ostrze Burzy";
                description = "Razisz pobliskie potwory mocą burzy.";
                cooldown = 12;
            }
            case "dash" -> {
                material = Material.FEATHER;
                name = ChatColor.WHITE + "✦ Pióro Zefira";
                description = "Wykonujesz szybki zryw w kierunku patrzenia.";
                cooldown = 6;
            }
            case "heart" -> {
                material = Material.HEART_OF_THE_SEA;
                name = ChatColor.LIGHT_PURPLE + "♥ Serce Odnowy";
                description = "Odnawiasz 4 serca i otrzymujesz regenerację.";
                cooldown = 25;
            }
            case "miner" -> {
                material = Material.DIAMOND_PICKAXE;
                name = ChatColor.GOLD + "✦ Kilof Pradawnych";
                description = "Otrzymujesz Pośpiech III na 15 sekund.";
                cooldown = 30;
            }
            default -> throw new IllegalArgumentException("Nieznany item: " + id);
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(java.util.List.of(
                ChatColor.DARK_GRAY + "AUTORSKI ARTEFAKT",
                "",
                ChatColor.GRAY + description,
                "",
                ChatColor.YELLOW + "PPM " + ChatColor.GRAY + "— aktywuj moc",
                ChatColor.GRAY + "Cooldown: " + ChatColor.AQUA + cooldown + " s",
                ChatColor.DARK_GRAY + "GUI przedmiotów: Shift + F"
        ));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String id = item.getItemMeta().getPersistentDataContainer()
                .get(itemKey, PersistentDataType.STRING);

        if (id == null) {
            return;
        }

        int seconds = switch (id) {
            case "storm" -> 12;
            case "dash" -> 6;
            case "heart" -> 25;
            case "miner" -> 30;
            default -> 0;
        };

        if (seconds == 0) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();

        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(
                player.getUniqueId(), key -> new HashMap<>()
        );

        long remaining = playerCooldowns.getOrDefault(id, 0L) - now;
        if (remaining > 0) {
            player.sendMessage(ChatColor.RED + "Moc ładuje się jeszcze "
                    + ((remaining + 999) / 1000) + " s.");
            return;
        }

        if (!activate(player, id)) {
            return;
        }

        playerCooldowns.put(id, now + seconds * 1000L);
    }

    private boolean activate(Player player, String id) {
        Location location = player.getLocation();

        switch (id) {
            case "storm" -> {
                int hits = 0;

                for (Entity entity : player.getNearbyEntities(5, 3, 5)) {
                    // Moc nie atakuje graczy, zwierząt ani oswojonych pupili.
                    if (!(entity instanceof org.bukkit.entity.Monster monster)) {
                        continue;
                    }

                    if (monster.isDead() || !player.hasLineOfSight(monster)) {
                        continue;
                    }

                    monster.getWorld().strikeLightningEffect(monster.getLocation());
                    monster.damage(8.0, player);
                    hits++;
                }

                if (hits == 0) {
                    player.sendMessage(ChatColor.GRAY + "Brak widocznych potworów w pobliżu.");
                    return false;
                }

                player.getWorld().spawnParticle(
                        Particle.ELECTRIC_SPARK, location.clone().add(0, 1, 0),
                        60, 1.5, 0.7, 1.5, 0.1
                );
                player.sendMessage(ChatColor.AQUA + "Burza poraziła " + hits + " potworów!");
            }
            case "dash" -> {
                Vector direction = player.getEyeLocation().getDirection().multiply(1.5);
                direction.setY(Math.max(0.25, Math.min(0.8, direction.getY())));
                player.setVelocity(direction);

                player.getWorld().spawnParticle(
                        Particle.CLOUD, location.clone().add(0, 0.5, 0),
                        30, 0.4, 0.3, 0.4, 0.05
                );
                player.playSound(location, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 1.8f);
            }
            case "heart" -> {
                AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
                if (health == null || player.isDead()) {
                    return false;
                }

                player.setHealth(Math.min(health.getValue(), player.getHealth() + 8.0));
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.REGENERATION, 100, 0)
                );

                player.getWorld().spawnParticle(
                        Particle.HEART, location.clone().add(0, 1.5, 0),
                        12, 0.6, 0.5, 0.6, 0
                );
                player.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            }
            case "miner" -> {
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.HASTE, 300, 2)
                );
                player.getWorld().spawnParticle(
                        Particle.ENCHANT, location.clone().add(0, 1, 0),
                        40, 0.5, 0.6, 0.5, 0.3
                );
                player.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1f);
                player.sendMessage(ChatColor.GOLD + "Pradawna moc! Pośpiech III na 15 sekund.");
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Zachowujemy aktywne cooldowny, aby relog ich nie resetował.
        long now = System.currentTimeMillis();
        cooldowns.values().forEach(values ->
                values.entrySet().removeIf(entry -> entry.getValue() <= now)
        );
        cooldowns.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private static final class CustomMenu implements InventoryHolder {

        private final Inventory inventory;

        private CustomMenu() {
            inventory = Bukkit.createInventory(
                    this, 27, ChatColor.DARK_AQUA + "✦ Autorskie artefakty ✦"
            );
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}