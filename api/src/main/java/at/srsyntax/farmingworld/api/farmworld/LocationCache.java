package at.srsyntax.farmingworld.api.farmworld;

import com.google.gson.Gson;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/*
 * MIT License
 *
 * Copyright (c) 2022-2024 Marcel Haberl
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

/**
 * A class to store a Bukkit location in JSON format and convert it back to a Bukkit location.
 */
@Getter
public class LocationCache extends SpawnLocation {

    protected final String world;

    public LocationCache(Location location) {
        super(location);
        this.world = location.getWorld().getName();
    }

    public static LocationCache fromJson(String json) {
        return new Gson().fromJson(json, LocationCache.class);
    }

    public Location toBukkit() {
        return new Location(
                Bukkit.getWorld(this.world),
                this.x, this.y, this.z,
                this.yaw, this.pitch
        );
    }
}
