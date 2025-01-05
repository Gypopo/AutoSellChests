package me.gypopo.autosellchests.util.scheduler.tasks;

import me.gypopo.autosellchests.util.scheduler.Task;

public class BukkitTask implements Task {

    private final org.bukkit.scheduler.BukkitTask task;

    public BukkitTask(org.bukkit.scheduler.BukkitTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }
}