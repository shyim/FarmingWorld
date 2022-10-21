package at.srsyntax.farmingworld.config;

import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

/*
 * MIT License
 *
 * Copyright (c) 2022 Marcel Haberl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
@AllArgsConstructor
@Getter @Setter
public class PluginConfig {

    private String version;
    private final CountdownConfig countdown;
    private final MessageConfig messages;

    public PluginConfig(Plugin plugin) {
        this(
                plugin.getDescription().getVersion(),
                new CountdownConfig(
                        5,
                        .7D,
                        false,
                        ChatMessageType.ACTION_BAR
                ),
                new MessageConfig(
                        new MessageConfig.CountdownMessages(
                                "&cA countdown is already underway.",
                                "&cThe countdown was interrupted because you moved.",
                                "&7You will be teleported in &e%s &7seconds."
                        )
                )
        );
    }

    public void save(Plugin plugin) throws IOException {
        final File file = new File(plugin.getDataFolder(), ConfigLoader.CONFIG_FILENAME);

        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        if (!file.exists()) file.createNewFile();

        final String json = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
                .toJson(this);
        Files.write(
                file.toPath(),
                Arrays.asList(json.split("\n")),
                StandardCharsets.UTF_8
        );
    }

    @AllArgsConstructor
    @Getter
    public static class CountdownConfig {
        private final int time;
        private final double permittedDistance;
        private final boolean movementAllowed;
        private final ChatMessageType messageType;
    }

}
