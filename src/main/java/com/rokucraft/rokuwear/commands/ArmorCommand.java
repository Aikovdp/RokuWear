package com.rokucraft.rokuwear.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.paper.PaperCommandManager;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent.SlotType;
import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Orientable;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.rokucraft.rokuwear.RokuWear;
import com.rokucraft.rokuwear.util.GuiUtil;
import com.rokucraft.rokuwear.wearables.ArmorSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public class ArmorCommand {
    private final RokuWear plugin;

    public ArmorCommand(RokuWear plugin) {
        this.plugin = plugin;
    }

    public void register(PaperCommandManager<CommandSender> manager) {
        manager.command(
                manager.commandBuilder("armor", ArgumentDescription.of("Shows your available armors"))
                        .senderType(Player.class)
                        .handler(context -> {
                            final Player player = (Player) context.getSender();
                            List<ArmorSet> unlockedArmorSets = new ArrayList<>();
                            for (ArmorSet set : plugin.config().armorSets()) {
                                if (player.hasPermission(set.permission())) {
                                    unlockedArmorSets.add(set);
                                }
                            }


                            if (unlockedArmorSets.isEmpty())
                                player.sendMessage(Component.text("You do not own any armor!", NamedTextColor.RED));
                            else
                                buildGui(unlockedArmorSets, player).show(player);
                        }).build());
    }

    private ChestGui buildGui(List<ArmorSet> armorSets, Player player) {
        ChestGui gui = new ChestGui(5, ComponentHolder.of(Component.text("Armor")));
        gui.setOnGlobalClick(event -> event.setCancelled(true));

        PaginatedPane pages = toPages(armorSets);
        gui.addPane(pages);
        gui.addPane(GuiUtil.getHeader(player));
        gui.addPane(GuiUtil.getRemoveButton(
                "Remove Armor", 4, gui.getRows() - 1,
                event -> {
                    PlayerInventory inventory = event.getWhoClicked().getInventory();
                    inventory.setChestplate(null);
                    inventory.setLeggings(null);
                    inventory.setBoots(null);
                    event.getWhoClicked().sendMessage(Component.text("Your armor was removed", NamedTextColor.GREEN));
                }
        ));
        if (pages.getPages() > 1)
            gui.addPane(GuiUtil.getNavigation(gui, pages));

        return gui;
    }

    private GuiItem guiItem(ArmorSet.Piece piece) {
        ItemStack pieceItem = piece.toItemStack();
        return new GuiItem(pieceItem,
                event -> {
                    PlayerInventory inventory = event.getWhoClicked().getInventory();
                    SlotType slotType = SlotType.getByMaterial(pieceItem.getType());
                    if (slotType == null) return;
                    switch (slotType) {
                        case HEAD -> inventory.setHelmet(pieceItem);
                        case CHEST -> inventory.setChestplate(pieceItem);
                        case LEGS -> inventory.setLeggings(pieceItem);
                        case FEET -> inventory.setBoots(pieceItem);
                    }
                    event.getWhoClicked().sendMessage(Component.text().append(
                            Component.text("Equipped ", NamedTextColor.GREEN),
                            piece.name().hoverEvent(pieceItem.asHoverEvent())
                    ));
                }
        );
    }

    private PaginatedPane toPages(List<ArmorSet> armorSets) {
        final int width = 7;
        final int height = 3;
        final int itemsPerPage = width * height;

        List<ArmorSet.Piece> pieces = new ArrayList<>();
        for (ArmorSet armorSet : armorSets)
            pieces.addAll(armorSet.pieces());


        PaginatedPane pages = new PaginatedPane(1, 1, width, height);
        int pagesNeeded = (pieces.size() + itemsPerPage - 1) / itemsPerPage;

        int index = 0;
        for (int i = 0; i < pagesNeeded; i++) {
            OutlinePane outlinePane = new OutlinePane(width, height);
            outlinePane.setOrientation(Orientable.Orientation.VERTICAL);
            for (int j = 0; j < itemsPerPage; j++) {
                if (index >= pieces.size()) break;
                outlinePane.addItem(guiItem(pieces.get(index++)));
            }
            pages.addPane(i, outlinePane);
        }
        return pages;
    }
}
