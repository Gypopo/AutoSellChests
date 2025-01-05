package me.gypopo.autosellchests.util.scheduler.tasks;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.gypopo.autosellchests.util.scheduler.Task;

public class FoliaTask implements Task {

    private final ScheduledTask task;

    public FoliaTask(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }
}