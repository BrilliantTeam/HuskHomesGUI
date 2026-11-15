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
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.gui.HuskHomesGui;
import net.william278.huskhomes.position.Home;
import net.william278.huskhomes.position.SavedPosition;
import net.william278.huskhomes.position.Warp;
import net.william278.huskhomes.user.OnlineUser;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Consumer;

public abstract class Menu {
    private static final String TAG_KEY = "huskhomesgui:icon";
    private static final String CUSTOM_MODEL_DATA_KEY = "huskhomesgui:custom_model_data";

    protected final HuskHomesGui plugin;
    protected final HuskHomesAPI api;
    private final InventoryGui gui;

    protected Menu(@NotNull HuskHomesGui plugin, @NotNull String title, @NotNull String[] layout) {
        this.plugin = plugin;
        this.api = HuskHomesAPI.getInstance();
        this.gui = new InventoryGui(plugin, title, layout);
    }

    protected abstract Consumer<InventoryGui> buildMenu();

    public final void show(@NotNull OnlineUser user) {
        buildMenu().accept(gui);
        gui.show(api.getPlayer(user));
    }

    public final void setPageNumber(@NotNull OnlineUser user, int pageNumber) {
        gui.setPageNumber(api.getPlayer(user), pageNumber);
    }

    public final int getPageNumber(@NotNull OnlineUser user) {
        return gui.getPageNumber(api.getPlayer(user));
    }

    public final void close(@NotNull OnlineUser user) {
        gui.close(api.getPlayer(user));
    }

    public final void destroy() {
        gui.destroy();
    }

    @NotNull
    protected static ItemStack icon(int model) {
        final ItemStack item = new ItemStack(Material.STICK);
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addFloat(model).build());
        return item;
    }

    @NotNull
    protected static ItemStack filler() {
        final ItemStack item = icon(20036);
        item.editMeta(meta -> meta.setHideTooltip(true));
        return item;
    }

    /**
     * Get the itemstack to use for a saved position by icon tag
     *
     * @param position The saved position
     * @return The itemstack to use
     */
    protected ItemStack getPositionItemStack(@NotNull SavedPosition position, @NotNull Material fallbackMaterial) {
        final Map<String, String> tags = position.getMeta().getTags();
        final Material material = tags.containsKey(TAG_KEY) ? Material.matchMaterial(tags.get(TAG_KEY)) : null;
        final ItemStack itemStack = new ItemStack(material != null && material.isItem() ? material : fallbackMaterial);
        if (tags.containsKey(CUSTOM_MODEL_DATA_KEY)) {
            try {
                itemStack.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData()
                        .addFloat(Float.parseFloat(tags.get(CUSTOM_MODEL_DATA_KEY))).build());
            } catch (NumberFormatException ignored) {
            }
        }
        return itemStack;
    }

    /**
     * Set the material to use for a {@link SavedPosition} and update it in the database
     *
     * @param position The saved position
     * @param material The {@link Material} to use
     */
    protected void setPositionMaterial(@NotNull SavedPosition position, @NotNull ItemStack itemStack) {
        final Map<String, String> tags = position.getMeta().getTags();
        tags.put(TAG_KEY, itemStack.getType().getKey().toString());
        final CustomModelData model = itemStack.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (model != null && !model.floats().isEmpty()) {
            tags.put(CUSTOM_MODEL_DATA_KEY, String.valueOf(model.floats().getFirst()));
        } else {
            tags.remove(CUSTOM_MODEL_DATA_KEY);
        }

        if (position instanceof Warp warp) {
            api.setWarpMetaTags(warp, tags);
        } else if (position instanceof Home home) {
            api.setHomeMetaTags(home, tags);
        } else {
            throw new IllegalArgumentException("Position must be a warp or home");
        }
    }

    /**
     * Represents different types of {@link SavedPosition} that a {@link ListMenu} can display
     */
    protected enum Type {
        HOME,
        PUBLIC_HOME,
        WARP
    }

}
