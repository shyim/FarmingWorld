package at.srsyntax.farmingworld.farmworld.scheduler;

import at.srsyntax.farmingworld.api.farmworld.FarmWorld;

/*
 * MIT License
 *
 * Copyright (c) 2022-2023 Marcel Haberl
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
public class FarmWorldUpdater implements Runnable {
    private final FarmWorldScheduler scheduler;
    protected int taskId = -1;

    public FarmWorldUpdater(FarmWorldScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        for (FarmWorld farmWorld : scheduler.updaterList) {
            update(farmWorld, true);
        }
    }

    public void update(FarmWorld farmWorld, boolean everySecond) {
        if (!farmWorld.isActive()) {
            removeUpdater(farmWorld, everySecond);
            return;
        }

        if (farmWorld.needReset()) {
            farmWorld.next();
            removeUpdater(farmWorld, everySecond);
        } else if (farmWorld.needNextWorld())  {
            scheduler.plugin.getLogger().info("Create new world for " + farmWorld.getName());
            farmWorld.newNextWorld(farmWorld.generateWorld());
        }

        farmWorld.updateSigns();
        final var displayer = scheduler.plugin.getDisplayRegistry().getDisplayer(farmWorld);
        if (displayer != null) displayer.display();
    }

    private void removeUpdater(FarmWorld farmWorld, boolean everySecond) {
        if (everySecond) scheduler.removeUpdater(farmWorld);
    }
}
