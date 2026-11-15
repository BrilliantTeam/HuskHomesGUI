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

package net.william278.huskhomes.gui.config;

import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@YamlFile(header = """
        ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
        ┃     HuskHomesGUI Config      ┃
        ┃    Developed by William278   ┃
        ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
        ┗╸ Information: https://william278.net/project/huskhomesgui""")
public class Settings {

    @YamlKey("language")
    private String language = "en-gb";

    @YamlComment("Options for the home/warp list menu GUI")
    @YamlKey("menu.rows")
    private int menuSize = 4;
    @YamlKey("menu.show_controls")
    private boolean showMenuControls = true;
    @YamlKey("menu.display_controls_help_in_lore")
    private boolean displayControlsHelpInCore = false;
    @YamlKey("menu.text_wrap_length")
    private int textWrapLength = 17;

    @YamlKey("menu.items.default_icon")
    private String defaultIcon = "minecraft:stone";

    @SuppressWarnings("unused")
    private Settings() {
    }

    @NotNull
    public String getLanguage() {
        return language;
    }

    public int getMenuSize() {
        return Math.max(2, Math.min(menuSize, 6));
    }

    public boolean doShowMenuControls() {
        return showMenuControls;
    }

    public boolean camelCase() {
        return displayControlsHelpInCore;
    }

    public int getTextWrapLength() {
        return textWrapLength;
    }

    @NotNull
    public Material getDefaultIcon() {
        return getMaterial(defaultIcon);
    }

    @NotNull
    private Material getMaterial(@NotNull String id) {
        id = id.replace("minecraft:", "");
        return Optional.ofNullable(Material.matchMaterial(id)).orElse(Material.STONE);
    }
}
