package me.gypopo.autosellchests.scheduler;

import me.gypopo.autosellchests.AutoSellChests;
import me.gypopo.autosellchests.managers.UpgradeManager;
import me.gypopo.autosellchests.objects.Chest;

import java.util.*;

public class SchedulerQueue {

    private final PriorityQueue<Chest> chests = new PriorityQueue<>(Comparator.comparingLong(Chest::getNextInterval));
    private final Map<Integer, List<SellPosition>> SELL_TIMES_PER_INTERVAL = new HashMap<>(); // The current time stamps of chests per interval upgrade

    public SchedulerQueue() {
        this.calculateQueue();
    }

    public void reload() {
        this.chests.clear();
        this.SELL_TIMES_PER_INTERVAL.clear();

        this.calculateQueue();
    }

    private void calculateQueue() {
        for (int i = 0; i < UpgradeManager.getDifferentIntervals(); i++) {
            this.scheduleFromInterval(i);
        }
    }

    // Load and schedule the chests per interval
    private void scheduleFromInterval(int intervalID) {
        ArrayList<Chest> CHESTS_BY_INTERVAL = new ArrayList<>();

        for (Chest chest : AutoSellChests.getInstance().getManager().getLoadedChests().values()) {
            if ((UpgradeManager.intervalUpgrades ? chest.getIntervalUpgrade() : 0) == intervalID)
                CHESTS_BY_INTERVAL.add(chest);
        }

        if (CHESTS_BY_INTERVAL.isEmpty())
            return;

        List<SellPosition> sellTimes = this.schedule(CHESTS_BY_INTERVAL, UpgradeManager.getIntervals()[intervalID]);
        sellTimes.sort(Comparator.comparingLong(SellPosition::sellTime));
        this.SELL_TIMES_PER_INTERVAL.put(intervalID, sellTimes);
    }

    // TODO: The last loaded interval will always be the maximum millis, where the first chest of each interval upgrade will always have a interval of 0, making them always overlap
    private List<SellPosition> schedule(ArrayList<Chest> CHESTS_BY_INTERVAL, long millis) {
        // Calculate the best time for when to sell a chest in ticks
        long currentTime = System.currentTimeMillis();
        long interval = millis / CHESTS_BY_INTERVAL.size();

        List<SellPosition> sellTimes = new ArrayList<>();
        for (int i = 0; i < CHESTS_BY_INTERVAL.size(); i++) {
            // Spread the sell time of this chest in the complete interval so not all chests have the same interval
            long nextInterval = ((i+1) * interval) % millis;

            Chest chest = CHESTS_BY_INTERVAL.get(i);
            chest.setNextInterval(currentTime + nextInterval);
            sellTimes.add(new SellPosition(nextInterval, chest.getId()));
            this.chests.offer(chest);
        }

        sellTimes.add(new SellPosition(millis, -1));
        return sellTimes;
    }

    public Chest getNextAndUpdate() {
        Chest chest = this.chests.poll();
        chest.setNextInterval(System.currentTimeMillis() + chest.getInterval());
        this.chests.offer(chest);

        return chest;
    }

    public Chest peek() {
        return this.chests.peek();
    }

    // When a new chest is created, add it dynamically to the scheduler and calculate the best possible sell time
    public void addChest(Chest chest) {
        SellPosition position = new SellPosition(this.getBestSellTime(UpgradeManager.intervalUpgrades ? chest.getIntervalUpgrade() : 0), chest.getId());
        chest.setNextInterval(System.currentTimeMillis() + position.sellTime);

        List<SellPosition> sellTimes = this.getSellTimes(UpgradeManager.intervalUpgrades ? chest.getIntervalUpgrade() : 0);
        sellTimes.add(position);
        sellTimes.sort(Comparator.comparingLong(SellPosition::sellTime));

        this.chests.offer(chest);
    }

    // When a chest is removed/destroyed
    public void removeChest(Chest chest) {
        this.getSellTimes(UpgradeManager.intervalUpgrades ? chest.getIntervalUpgrade() : 0)
                .removeIf(pos -> pos.chestID == chest.getId());
        this.chests.remove(chest);
    }

    // Add the chest to the queue again to update its position by its next interval
    public void updateChestInterval(Chest chest, int newIntervalID) {
        this.getSellTimes(chest.getIntervalUpgrade())
                .removeIf(pos -> pos.chestID == chest.getId());
        this.chests.remove(chest);

        SellPosition position = new SellPosition(this.getBestSellTime(newIntervalID), chest.getId());
        chest.setNextInterval(System.currentTimeMillis() + position.sellTime);
        chest.setInterval(UpgradeManager.getIntervals()[newIntervalID]);
        chest.setIntervalUpgrade(newIntervalID);

        List<SellPosition> sellTimes = this.getSellTimes(newIntervalID);
        sellTimes.add(position);
        sellTimes.sort(Comparator.comparingLong(SellPosition::sellTime));

        this.chests.offer(chest);
    }

    // Calculate a new sell time for its new interval
    // Instead of recalculating the whole queue for this interval,
    // this approach searches the first largest 'gap' between existing intervals
    // This prevents existing chests their sell times to change when a new chest is added
    private long getBestSellTime(int newIntervalID) {
        if (this.SELL_TIMES_PER_INTERVAL.getOrDefault(newIntervalID, new ArrayList<>()).isEmpty())
            return UpgradeManager.getIntervals()[newIntervalID] / 2;
        long largestGap = 0;
        long previousTime = 0;
        long previousLargestTime = 0;

        for (SellPosition position : this.SELL_TIMES_PER_INTERVAL.get(newIntervalID)) {
            long currentGapSize = position.sellTime - previousTime;
            if (currentGapSize > largestGap) {
                previousLargestTime = previousTime;
                largestGap = currentGapSize;
            }
            previousTime = position.sellTime;
        }

        return previousLargestTime + (largestGap / 2);
    }

    private List<SellPosition> getSellTimes(int intervalID) {
        return this.SELL_TIMES_PER_INTERVAL.computeIfAbsent(intervalID, k -> new ArrayList<>(Collections.singletonList(new SellPosition(UpgradeManager.getIntervals()[intervalID], -1))));
    }

    /**
     * @param sellTime The time to sell each interval
     */
    private record SellPosition(long sellTime, int chestID) {}

}