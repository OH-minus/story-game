package backend.storyweave;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

final class MutableClock extends Clock {
    private long currentMillis;

    MutableClock(long currentMillis) {
        this.currentMillis = currentMillis;
    }

    void advanceMillis(long millis) {
        currentMillis += millis;
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("UTC");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(currentMillis);
    }

    @Override
    public long millis() {
        return currentMillis;
    }
}