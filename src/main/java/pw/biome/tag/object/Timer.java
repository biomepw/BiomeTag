package pw.biome.tag.object;

import lombok.Getter;

public class Timer {

    @Getter
    private long timeBegin;

    @Getter
    private boolean currentlyTiming;

    @Getter
    private int timeSeconds;

    /**
     * Method to start timer if not currently timing
     */
    public void start() {
        if (!currentlyTiming) {
            timeBegin = System.currentTimeMillis();
            currentlyTiming = true;
        }
    }

    /**
     * Method to stop timer if currently timing
     */
    public void stop() {
        if (currentlyTiming) {
            timeSeconds = getTimeElapsed();
            currentlyTiming = false;
        }
    }

    public int getTimeElapsed() {
        return (int) (System.currentTimeMillis() - timeBegin) / 1000;
    }
}
