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

import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.william278.huskhomes.gui.HuskHomesGui;
import net.william278.huskhomes.position.Home;
import net.william278.huskhomes.position.SavedPosition;
import net.william278.huskhomes.position.Warp;
import net.william278.huskhomes.user.OnlineUser;
import net.william278.huskhomes.util.ValidationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static net.william278.huskhomes.gui.config.Locales.textWrap;

/**
 * A menu for editing a saved position
 */
public class EditMenu<T extends SavedPosition> extends Menu {

    private final T position;
    private final ListMenu<T> parentMenu;
    private final int pageNumber;

    @NotNull
    private static String[] getEditMenuLayout() {
        return new String[]{
                "        b",
                " undip r ",
                "         "
        };
    }

    private EditMenu(@NotNull HuskHomesGui plugin, @NotNull T position, @NotNull ListMenu<T> parentMenu, int pageNumber) {
        super(plugin, plugin.getLocales().getLocale(
                        position instanceof Home ? "home_editor_title" : "warp_editor_title", position.getName()),
                getEditMenuLayout());
        this.position = position;
        this.parentMenu = parentMenu;
        this.pageNumber = pageNumber;
    }

    public static EditMenu<Home> home(@NotNull HuskHomesGui plugin, @NotNull Home home,
                                      @NotNull ListMenu<Home> parentMenu,
                                      int pageNumber) {
        return new EditMenu<>(plugin, home, parentMenu, pageNumber);
    }

    public static EditMenu<Warp> warp(@NotNull HuskHomesGui plugin, @NotNull Warp warp,
                                      @NotNull ListMenu<Warp> parentMenu,
                                      int pageNumber) {
        return new EditMenu<>(plugin, warp, parentMenu, pageNumber);
    }

    @Override
    protected Consumer<InventoryGui> buildMenu() {
        return (menu) -> {
            final ItemStack positionIcon = getPositionItemStack(position, plugin.getSettings().getDefaultIcon());
            menu.setCloseAction(i -> false);

            menu.setFiller(filler());

            // Return to the parent list menu
            menu.addElement(new StaticGuiElement('b',
                    icon(20004),
                    (click) -> {
                        if (click.getWhoClicked() instanceof Player player) {
                            final OnlineUser user = api.adaptUser(player);
                            this.close(user);
                            parentMenu.show(user);
                            parentMenu.setPageNumber(user, pageNumber);
                            this.destroy();
                        }
                        return true;
                    },
                    plugin.getLocales().getLocale("back_button")));

            // Relocating
            menu.addElement(new StaticGuiElement('u',
                    icon(20058),
                    (click) -> {
                        if (click.getWhoClicked() instanceof Player player) {
                            try {
                                if (position instanceof Home home) {
                                    api.relocateHome(home, api.adaptUser(player).getPosition());
                                } else if (position instanceof Warp warp) {
                                    api.relocateWarp(warp, api.adaptUser(player).getPosition());
                                }
                            } catch (ValidationException e) {
                                return true;
                            }
                        }
                        return true;
                    },
                    plugin.getLocales().getLocale("edit_location_button"),
                    plugin.getLocales().getLocale("edit_location_default_message",
                            Integer.toString((int) Math.floor(position.getX())),
                            Integer.toString((int) Math.floor(position.getY())),
                            Integer.toString((int) Math.floor(position.getZ())))));

            // Editing name
            menu.addElement(new StaticGuiElement('n',
                    icon(21021),
                    (click) -> {
                        if (click.getWhoClicked() instanceof Player player) {
                            this.close(api.adaptUser(player));
                            prompt(player, plugin.getLocales().getLocale("edit_name_title", position.getName()),
                                    plugin.getLocales().getLocale("edit_name_button"), positionIcon,
                                    position.getName(), text -> {
                                        try {
                                            if (position instanceof Home home) {
                                                api.renameHome(home, text);
                                            } else if (position instanceof Warp warp) {
                                                api.renameWarp(warp, text);
                                            }
                                        } catch (ValidationException e) {
                                            return false;
                                        }
                                        position.getMeta().setName(text);

                                        // Refresh menu title
                                        this.destroy();
                                        new EditMenu<>(plugin, position, parentMenu, pageNumber).show(api.adaptUser(player));
                                        return true;
                                    });
                        }
                        return true;
                    },
                    plugin.getLocales().getLocale("edit_name_button")));

            // Editing description
            menu.addElement(new StaticGuiElement('d',
                    icon(21118),
                    (click) -> {
                        if (click.getWhoClicked() instanceof Player player) {
                            this.close(api.adaptUser(player));
                            prompt(player, plugin.getLocales().getLocale("edit_description_title", position.getName()),
                                    plugin.getLocales().getLocale("edit_description_button"), positionIcon,
                                    // Description or default_description
                                    !position.getMeta().getDescription().isBlank() ?
                                            position.getMeta().getDescription()
                                            : plugin.getLocales().getLocale("edit_description_default_input"),
                                    text -> {
                                        try {
                                            if (position instanceof Home home) {
                                                api.setHomeDescription(home, text);
                                            } else if (position instanceof Warp warp) {
                                                api.setWarpDescription(warp, text);
                                            }
                                        } catch (ValidationException e) {
                                            return false;
                                        }
                                        position.getMeta().setDescription(text);
                                        this.show(api.adaptUser(player));
                                        return true;
                                    });
                        }
                        return true;
                    },
                    plugin.getLocales().getLocale("edit_description_button"),

                    // description
                    (!position.getMeta().getDescription().isBlank() ?
                            plugin.getLocales().getLocale("edit_description_default_message", textWrap(plugin, position.getMeta().getDescription()))
                            : plugin.getLocales().getLocale("edit_description_default_message_blank"))));

            // Editing home privacy
            if (position instanceof Home home) {
                menu.addElement(new StaticGuiElement('p',
                        icon(home.isPublic() ? 20054 : 20056),
                        (click) -> {
                            if (click.getWhoClicked() instanceof Player player) {
                                try {
                                    api.setHomePrivacy(home, !home.isPublic());
                                    // Update the status display on the menu
                                    home.setPublic(!home.isPublic());
                                    this.show(api.adaptUser(player));
                                } catch (ValidationException e) {
                                    return true;
                                }
                            }
                            return true;
                        },
                        plugin.getLocales().getLocale("edit_privacy_button"),
                        plugin.getLocales().getLocale("edit_privacy_message", (home.isPublic() ?
                                plugin.getLocales().getLocale("edit_privacy_message_public")
                                : plugin.getLocales().getLocale("edit_privacy_message_private")))));
            }

            // Deleting
            menu.addElement(new StaticGuiElement('r',
                    icon(21002),
                    (click) -> {
                        switch (click.getType()) {
                            case RIGHT, DROP -> { // DROP: geyser player throw item
                                if (click.getWhoClicked() instanceof Player player) {
                                    this.close(api.adaptUser(player));
                                    try {
                                        if (position instanceof Home home) {
                                            api.deleteHome(home);
                                            home.getMeta().setName(plugin.getLocales().getLocale("item_deleted_name", home.getName())); // update listMenu
                                        } else if (position instanceof Warp warp) {
                                            api.deleteWarp(warp);
                                            warp.getMeta().setName(plugin.getLocales().getLocale("item_deleted_name", warp.getName())); // update listMenu
                                        }
                                    } catch (ValidationException e) {
                                        return true;
                                    }

                                    // Return to the parent list menu
                                    final OnlineUser user = api.adaptUser(player);
                                    this.close(user);
                                    parentMenu.show(user);
                                    parentMenu.setPageNumber(user, pageNumber);
                                    this.destroy();
                                }
                            }
                        }
                        return true;
                    },
                    plugin.getLocales().getLocale("delete_button"),
                    plugin.getLocales().getLocale("delete_button_describe")
            ));

            // Controls display
            menu.addElement(new StaticGuiElement('i',
                    icon(21135),
                    // Name
                    plugin.getLocales().getLocale("item_info_name", position.getName()),
                    // World name
                    plugin.getLocales().getLocale("item_info_world", position.getWorld().getName()),
                    // Server name
                    plugin.getLocales().getLocale("item_info_server", position.getServer()),
                    // Coordinates
                    plugin.getLocales().getLocale("item_info_coordinates",
                            Integer.toString((int) Math.floor(position.getX())),
                            Integer.toString((int) Math.floor(position.getY())),
                            Integer.toString((int) Math.floor(position.getZ()))),
                    // Owner name (Only for homes)
                    position instanceof Home home ? plugin.getLocales()
                            .getLocale("home_owner_name", home.getOwner().getName()) : ""
            ));
        };
    }

    private void prompt(@NotNull Player player, @NotNull String title, @NotNull String confirm,
                        @NotNull ItemStack icon, @NotNull String initial, @NotNull Predicate<String> submit) {
        final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
        final ClickCallback.Options once = ClickCallback.Options.builder()
                .uses(1).lifetime(ClickCallback.DEFAULT_LIFETIME).build();
        player.showDialog(Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(legacy.deserialize(title))
                        .canCloseWithEscape(false)
                        .body(List.of(DialogBody.item(icon).build()))
                        .inputs(List.of(DialogInput.text("text", Component.empty())
                                .labelVisible(false)
                                .initial(initial)
                                .maxLength(Math.max(256, initial.length()))
                                .build()))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(legacy.deserialize(confirm))
                                .action(DialogAction.customClick((view, audience) -> {
                                    final String text = view.getText("text");
                                    if (!submit.test(text)) {
                                        prompt(player, title, confirm, icon, text, submit);
                                    }
                                }, once))
                                .build(),
                        ActionButton.builder(legacy.deserialize(plugin.getLocales().getLocale("back_button")))
                                .action(DialogAction.customClick((view, audience) -> this.show(api.adaptUser(player)), once))
                                .build()))));
    }

}
