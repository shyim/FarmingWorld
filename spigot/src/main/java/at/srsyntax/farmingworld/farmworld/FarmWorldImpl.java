package at.srsyntax.farmingworld.farmworld;

import at.srsyntax.farmingworld.FarmingWorldPlugin;
import at.srsyntax.farmingworld.api.event.farmworld.FarmWorldChangeWorldEvent;
import at.srsyntax.farmingworld.api.farmworld.Border;
import at.srsyntax.farmingworld.api.farmworld.FarmWorld;
import at.srsyntax.farmingworld.api.farmworld.SpawnLocation;
import at.srsyntax.farmingworld.api.farmworld.sign.SignCache;
import at.srsyntax.farmingworld.api.handler.cooldown.Cooldown;
import at.srsyntax.farmingworld.api.handler.countdown.Countdown;
import at.srsyntax.farmingworld.api.handler.countdown.CountdownCallback;
import at.srsyntax.farmingworld.api.template.TemplateData;
import at.srsyntax.farmingworld.database.repository.FarmWorldRepository;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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
public class FarmWorldImpl implements FarmWorld {

    @Setter @Getter
    private transient FarmingWorldPlugin plugin;

    private final String name;
    private final String permission;

    private final int cooldown, timer;
    private final double price;

    private final World.Environment environment;
    private final String generator;
    private final Border border;
    @Getter
    private boolean active = false;
    @Getter
    private final List<String> aliases;

    @Getter @Setter
    private transient FarmWorldData data;
    @Getter @Setter
    private transient boolean loaded = false, enabled = false;
    @Getter @Setter private transient LinkedHashMap<String, Location> locations = new LinkedHashMap<>();
    @Getter private transient String oldWorldName;
    @Getter private List<String> templates;
    private SpawnLocation spawn;

    public FarmWorldImpl(String name, String permission, int cooldown, int timer, double price, World.Environment environment, String generator, Border border, List<String> aliases) {
        this.name = name;
        this.permission = permission;
        this.cooldown = cooldown;
        this.timer = timer;
        this.price = price;
        this.environment = environment;
        this.generator = generator;
        this.border = border;
        this.aliases = aliases;
        this.data = new FarmWorldData(0, null, null);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    @Override
    public List<Player> getPlayers() {
        final var world = getWorld();
        if (world == null) return new ArrayList<>();
        return world.getPlayers();
    }

    @Override
    public boolean hasPermission(@NotNull Player player) {
        if (permission == null || permission.equalsIgnoreCase("null")) return true;
        return player.hasPermission("farmingworld.world.*") || player.hasPermission("farmingworld.world." + name);
    }

    @Override
    public void teleport(@NotNull Player... players) {
        teleport(false, players);
    }

    @Override
    public void teleport(boolean sameLocation, @NotNull Player... players) {
        final Location location = sameLocation ? randomLocation() : null;
        for (Player player : players)
            player.teleport(location == null ? randomLocation() : location);
    }

    @Override
    public void teleport(@NotNull List<Player> players) {
        teleport(false, players);
    }

    @Override
    public void teleport(boolean sameLocation, @NotNull List<Player> players) {
        teleport(sameLocation, players.toArray(Player[]::new));
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public Cooldown getCooldown(@NotNull Player player) {
        return FarmingWorldPlugin.getApi().getCooldown(player, this);
    }

    @Override
    public Countdown getCountdown(@NotNull Player player, @NotNull CountdownCallback callback) {
        return FarmingWorldPlugin.getApi().getCountdown(player, callback);
    }

    @Override
    public int getTimer() {
        return timer;
    }

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public @Nullable Location getSpawn() {
        try {
            final World world = getWorld();
            if (world != null) {
                final var file = new File(world.getWorldFolder(), "spawn.json");
                if (file.exists()) {
                    final var location = new Gson().fromJson(new FileReader(file), SpawnLocation.class).toBukkit(this);
                    if (location != null) {
                        return location;
                    }
                }
            }
        } catch (Exception ignored) {}
        return spawn == null ? null : spawn.toBukkit(this);
    }

    @Override
    public void setSpawn(Location location) {
        this.spawn = location == null ? null : new SpawnLocation(location);
        save(plugin);
    }

    @Override
    public boolean teleportSpawn(@NotNull Player player) {
        if (!player.isOnline()) return false;
        var location = getSpawn();
        if (location == null)
            location = randomLocation();
        player.teleport(location);
        return true;
    }

    @Override
    public boolean hasSpawn() {
        return spawn != null;
    }

    @Override
    public void setActive(boolean active) {
        if (this.active == active) return;
        this.active = active;
        if (!active) new FarmWorldDeleter(plugin, this).disable();
        else new FarmWorldLoader(plugin, this).enable();
        plugin.getSignRegistry().getCaches(this).forEach(SignCache::update);
    }

    @SneakyThrows
    @Override
    public void delete() {
        new FarmWorldDeleter(plugin, this).delete();
    }

    @Override
    public World.Environment getEnvironment() {
        return environment;
    }

    @Override
    public String getGenerator() {
        return generator;
    }

    @Override
    public @Nullable Border getBorder() {
        return border;
    }

    @Override
    public long getResetDate() {
        return data.getCreated() + TimeUnit.MINUTES.toMillis(timer);
    }

    @Override
    public long getCreated() {
        return data.getCreated();
    }

    @Override
    public boolean needReset() {
        if (data.getCurrentWorldName() == null) return true;
        return getResetDate() <= System.currentTimeMillis();
    }

    @Override
    public boolean needNextWorld() {
        return data.getCreated() + TimeUnit.MINUTES.toMillis(timer-1) <= System.currentTimeMillis() && !hasNext();
    }

    @SneakyThrows
    public void save(FarmingWorldPlugin plugin) {
        final FarmWorldRepository repository = plugin.getDatabase().getFarmWorldRepository();
        repository.save(this);
        plugin.getPluginConfig().save(plugin);
    }

    @Override
    public @Nullable World getWorld() {
        return data.getCurrentWorldName() == null ? null : new FarmWorldLoader(plugin, this).generateWorld(data.getCurrentWorldName());
    }

    @Override
    public void newWorld(@Nullable World nextWorld) {
        final World world = getWorld();
        data.setCurrentWorldName(nextWorld == null ? null : nextWorld.getName());
        data.setCreated(TimeUnit.MINUTES.toMillis(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis())));

        Bukkit.getPluginManager().callEvent(new FarmWorldChangeWorldEvent(this, world, getWorld()));

        if (locations == null) locations = new LinkedHashMap<>();
        if (!locations.isEmpty()) {
            plugin.getDatabase().getLocationRepository().deleteByFarmWorldName(name);
            locations.clear();
        }

        new FarmWorldLoader(plugin, this).checkLocations();
        if (nextWorld != null && world != null)
            teleport(world.getPlayers());

        new FarmWorldDeleter(plugin, this).deleteWorld(world);
        save(plugin);
    }

    @Override
    public @Nullable World getNextWorld() {
        return data.getNextWorldName() == null ? null : Bukkit.getWorld(data.getNextWorldName());
    }

    @Override
    public void newNextWorld(@Nullable World world) {
        data.setNextWorldName(world == null ? null : world.getName());
    }

    @Override
    public void deleteNextWorld() {
        if (!hasNext()) return;
        new FarmWorldDeleter(plugin, this).deleteWorld(getNextWorld());
    }

    @Override
    public void next() {
        var world = hasNext() ? getNextWorld() : generateWorld();
        if (world == null) world = generateWorld();
        data.setNextWorldName(null);
        next(world);
    }

    @Override
    public void next(@NotNull World world) {
        newWorld(world);
    }

    @Override
    public boolean hasNext() {
        return data.getNextWorldName() != null;
    }

    @Override
    public @NotNull World generateWorld() {
        return new FarmWorldLoader(plugin, this).generateWorld();
    }

    public WorldCreator createWorldCreator(String worldName) {
        final WorldCreator creator = new WorldCreator(worldName);
        creator.environment(environment);
        if (generator != null) creator.generator(generator);
        return creator;
    }

    @Override
    public void addLocation(String id, Location location) {
        if (locations == null) locations = new LinkedHashMap<>();
        locations.put(id, location);
    }

    @Override
    public void removeLocation(String id) {
        locations.remove(id);
        plugin.getDatabase().getLocationRepository().delete(id);
    }

    @Override
    public Location randomLocation() {
        if (locations.isEmpty())
            return new FarmWorldLoader(plugin, this).generateLocation(true);
        final Map.Entry<String, Location> location = locations.entrySet().stream().findFirst().get();
        removeLocation(location.getKey());
        if (locations.size() < plugin.getPluginConfig().getLocationCache())
            new FarmWorldLoader(plugin, this).generateLocation(true);
        return location.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FarmWorldImpl farmWorld = (FarmWorldImpl) o;
        return cooldown == farmWorld.cooldown && timer == farmWorld.timer
                && active == farmWorld.active && loaded == farmWorld.loaded && enabled == farmWorld.enabled
                && Objects.equals(name, farmWorld.name) && Objects.equals(permission, farmWorld.permission)
                && environment == farmWorld.environment && Objects.equals(generator, farmWorld.generator)
                && Objects.equals(border, farmWorld.border) && Objects.equals(aliases, farmWorld.aliases)
                && Objects.equals(data, farmWorld.data) && Objects.equals(locations, farmWorld.locations)
                && Objects.equals(oldWorldName, farmWorld.oldWorldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, permission, cooldown, timer, environment, generator, border, active, aliases, data, loaded, enabled, locations, oldWorldName);
    }

    @Override
    public void updateSigns() {
        plugin.getSignRegistry().update(this);
    }

    @Override
    public List<SignCache> getSigns() {
        return plugin.getSignRegistry().getCaches(this);
    }

    @Override
    public boolean hasTemplate() {
        return templates != null && !templates.isEmpty();
    }

    @Override
    public @NotNull TemplateData randomTemplate() {
        if (!hasTemplate()) throw new NullPointerException();
        final var template = templates.get(ThreadLocalRandom.current().nextInt(0, templates.size()));;
        final var data = plugin.getTemplateRegistry().getTemplate(template);
        if (data == null) throw new NullPointerException();
        return data;
    }

    @Override
    public void newWorld(@NotNull TemplateData data) {
        newWorld(new FarmWorldLoader(plugin, this).generateWorld(data));
    }

    @Override
    public void newNextWorld(TemplateData data) {
        newNextWorld(new FarmWorldLoader(plugin, this).generateWorld(data));
    }
}
