/*
 * This file is part of HuskHomesGUI, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskhomes.gui.menu;

import de.themoep.inventorygui.*;
import net.william278.huskhomes.gui.HuskHomesGui;
import net.william278.huskhomes.position.Home;
import net.william278.huskhomes.position.SavedPosition;
import net.william278.huskhomes.position.Warp;
import net.william278.huskhomes.teleport.TeleportationException;
import net.william278.huskhomes.user.OnlineUser;
import net.william278.huskhomes.user.User;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.io.File;
import java.util.stream.Collectors;

/**
 * A menu for displaying a list of saved positions
 */
public class ListMenu<T extends SavedPosition> extends Menu {

    private static final String EDIT_HOME_PERMISSION = "huskhomes.command.edithome";
    private static final String EDIT_HOME_OTHER_PERMISSION = "huskhomes.command.edithome.other";
    private static final String EDIT_WARP_PERMISSION = "huskhomes.command.editwarp";
    private final List<T> positions;
    private final Type type;
    private final int pageNumber = 1;
    private boolean filterByServer = false; // Flag to control server filtering

    @NotNull
    public static ListMenu<Home> homes(@NotNull HuskHomesGui plugin, @NotNull List<Home> homes, @NotNull User owner) {
        return new ListMenu<>(plugin, homes, Type.HOME,
                plugin.getLocales().getLocale("homes_menu_title", owner.getUsername()));
    }

    @NotNull
    public static ListMenu<Home> publicHomes(@NotNull HuskHomesGui plugin, @NotNull List<Home> homes) {
        return new ListMenu<>(plugin, homes, Type.PUBLIC_HOME,
                plugin.getLocales().getLocale("public_homes_menu_title"));
    }

    @NotNull
    public static ListMenu<Warp> warps(@NotNull HuskHomesGui plugin, @NotNull List<Warp> warps) {
        return new ListMenu<>(plugin, warps, Type.WARP,
                plugin.getLocales().getLocale("warps_menu_title"));
    }

    private ListMenu(@NotNull HuskHomesGui plugin, @NotNull List<T> positions, @NotNull ListMenu.Type type, @NotNull String title) {
        super(plugin, title, getMenuLayout(plugin));
        this.positions = positions;
        this.type = type;
    }

    @NotNull
    private static String[] getMenuLayout(@NotNull HuskHomesGui plugin) {
        return Arrays.copyOfRange(new String[]{
                        "ppppppppp",
                        "ppppppppp",
                        "ppppppppp",
                        "ppppppppp",
                        "ppppppppp",
                        "cl  i  nf"},
                6 - plugin.getSettings().getMenuSize(), 6);
    }

    @Override
    protected Consumer<InventoryGui> buildMenu() {
        return (menu) -> {
            // Set filler to STICK with custom model data 20036
            ItemStack fillerStick = new ItemStack(Material.STICK);
            ItemMeta fillerMeta = fillerStick.getItemMeta();
            if (fillerMeta != null) {
                fillerMeta.setCustomModelData(20036);
            }
            fillerStick.setItemMeta(fillerMeta);
            menu.setFiller(fillerStick);

            // Add button at row 6, slot 1: STICK with CMD 20004, executes /gmp gui open 傳送功能.yml
            menu.addElement(new StaticGuiElement('c',
                    createCustomStick(20004),
                    (click) -> {
                        if (click.getWhoClicked() instanceof Player player) {
                            player.performCommand("gmp gui open 傳送功能.yml");
                        }
                        return true;
                    },
                    plugin.getLocales().getLocale("teleport_function_button")));

            // Add button at row 6, slot 9: STICK with CMD 20053, toggles server filter
            menu.addElement(new DynamicGuiElement('f', (viewer) -> new StaticGuiElement('f',
                    createCustomStick(20053),
                    (click) -> {
                        if (click.getWhoClicked() instanceof Player player) {
                            filterByServer = !filterByServer; // Toggle filter flag
                            // Rebuild the position group with filtered positions
                            menu.removeElement('p');
                            menu.addElement(getPositionGroup(plugin, positions, menu));
                            menu.setPageNumber(0); // Reset to first page
                            menu.draw(); // Redraw menu to update display
                        }
                        return true;
                    },
                    plugin.getLocales().getLocale(filterByServer ? "filter_server_all" : "filter_server_current"))));

            // Add pagination handling
            menu.addElement(getPositionGroup(plugin, positions, menu));
            menu.addElement(new GuiPageElement('b',
                    new ItemStack(plugin.getSettings().getPaginateFirstPage()),
                    GuiPageElement.PageAction.FIRST,
                    plugin.getLocales().getLocale("pagination_first_page")));
            menu.addElement(new GuiPageElement('l',
                    createCustomStick(20037),
                    GuiPageElement.PageAction.PREVIOUS,
                    plugin.getLocales().getLocale("pagination_previous_page")));
            menu.addElement(new GuiPageElement('n',
                    createCustomStick(20048),
                    GuiPageElement.PageAction.NEXT,
                    plugin.getLocales().getLocale("pagination_next_page")));
            menu.addElement(new GuiPageElement('e',
                    new ItemStack(plugin.getSettings().getPaginateLastPage()),
                    GuiPageElement.PageAction.LAST,
                    plugin.getLocales().getLocale("pagination_last_page")));
            menu.setPageNumber(pageNumber);

            // Add controls information
            if (plugin.getSettings().doShowMenuControls()) {
                menu.addElement(new StaticGuiElement('i',
                        new ItemStack(plugin.getSettings().getControlsIcon()),
                        plugin.getLocales().getLocale("menu_controls_title"),
                        plugin.getLocales().getLocale("menu_controls_details")));
            }
        };
    }

    // Helper method: Create a STICK with specified custom model data
    @NotNull
    private ItemStack createCustomStick(int customModelData) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
        }
        stick.setItemMeta(meta);
        return stick;
    }

    /**
     * 從 plugins/HuskHomes/server.yml 讀取伺服器名稱
     */
    @NotNull
    private String getServerNameFromConfig(@NotNull HuskHomesGui plugin) {
        File configFile = new File(plugin.getDataFolder().getParentFile(), "HuskHomes/server.yml");
        if (!configFile.exists()) {
            plugin.getLogger().warning("server.yml not found, falling back to default server name");
            return "default";
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String serverName = config.getString("name", "default");
        return serverName != null ? serverName : "default";
    }

    // Modified getPositionGroup to support server filtering
    @NotNull
    private GuiElementGroup getPositionGroup(@NotNull HuskHomesGui plugin, @NotNull List<T> positions, @NotNull InventoryGui menu) {
        final GuiElementGroup group = new GuiElementGroup('p');
        List<T> filteredPositions = filterByServer ?
                positions.stream()
                        .filter(position -> position.getServer().equals(getServerNameFromConfig(plugin)))
                        .collect(Collectors.toList()) :
                positions;
        filteredPositions.forEach(position -> group.addElement(getPositionButton(plugin, position)));
        return group;
    }

    // Get a position select button for a SavedPosition
    @SuppressWarnings("unchecked")
    @NotNull
    private DynamicGuiElement getPositionButton(@NotNull HuskHomesGui plugin, @NotNull SavedPosition position) {
        return new DynamicGuiElement('e', (viewer) -> new StaticGuiElement('e',
                new ItemStack(getPositionItemStack(position, plugin.getSettings().getDefaultIcon())),
                (click) -> {
                    if (click.getWhoClicked() instanceof Player player) {
                        final OnlineUser user = api.adaptUser(player);
                        switch (click.getType()) {
                            case LEFT -> {
                                final ItemStack newItem = player.getItemOnCursor();
                                if (newItem.getType() == Material.AIR) {
                                    // teleport
                                    this.close(user);
                                    this.destroy();

                                    try {
                                        api.teleportBuilder(user)
                                                .target(position)
                                                .toTimedTeleport()
                                                .execute();
                                    } catch (TeleportationException ignored) {
                                    }
                                    return true;
                                }

                                // Update the icon with the item on the cursor
                                switch (type) {
                                    case HOME, PUBLIC_HOME -> {
                                        if (player.getUniqueId().equals(((Home) position).getOwner().getUuid())) {
                                            if (!player.hasPermission(EDIT_HOME_PERMISSION)) {
                                                return true;
                                            }
                                        } else {
                                            if (!player.hasPermission(EDIT_HOME_OTHER_PERMISSION)) {
                                                return true;
                                            }
                                        }
                                    }
                                    case WARP -> {
                                        if (!player.hasPermission(EDIT_WARP_PERMISSION)) {
                                            return true;
                                        }
                                    }
                                }
                                setPositionMaterial(position, newItem);
                                click.getGui().draw();
                            }

                            case RIGHT, DROP -> { // DROP: geyser player throw item
                                switch (type) {
                                    case WARP -> {
                                        if (!player.hasPermission(EDIT_WARP_PERMISSION)) {
                                            return true;
                                        }
                                    }
                                    case PUBLIC_HOME, HOME -> {
                                        if (position instanceof Home home) {
                                            if (!player.hasPermission(EDIT_HOME_PERMISSION)) {
                                                return true;
                                            }
                                            if (!player.getUniqueId().equals(home.getOwner().getUuid())
                                                    && !player.hasPermission(EDIT_HOME_OTHER_PERMISSION)) {
                                                return true;
                                            }
                                        }
                                    }
                                }
                                if (position instanceof Home home) {
                                    EditMenu.home(plugin, home, (ListMenu<Home>) this, getPageNumber(user)).show(user);
                                } else if (position instanceof Warp warp) {
                                    EditMenu.warp(plugin, warp, (ListMenu<Warp>) this, getPageNumber(user)).show(user);
                                }
                            }
                        }
                    }
                    return true;
                },

                // home name
                // Only use "item_name_public" for public home in home list
                ((type == Type.HOME && ((Home) position).isPublic()) ?
                        plugin.getLocales().getLocale("item_name_public", position.getName())
                        : plugin.getLocales().getLocale("item_name", position.getName())),

                // description
                (!position.getMeta().getDescription().isBlank() ?
                        plugin.getLocales().getLocale("item_description", position.getMeta().getDescription())
                        : plugin.getLocales().getLocale("item_description_blank")),

                // player name
                (position instanceof Home home ?
                        type == Type.PUBLIC_HOME ?
                                plugin.getLocales().getLocale("home_owner_name", home.getOwner().getUsername())
                                : ""
                        : ""),

                // item_controls
                (plugin.getSettings().camelCase() ?
                        plugin.getLocales().getLocale("item_controls")
                        : "")
        ));
    }

}