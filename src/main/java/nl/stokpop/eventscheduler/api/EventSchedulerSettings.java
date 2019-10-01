package nl.stokpop.eventscheduler.api;

import java.time.Duration;

public class EventSchedulerSettings {

    private final Duration keepAliveDuration;

    EventSchedulerSettings(Duration keepAliveDuration) {
        this.keepAliveDuration = keepAliveDuration;
    }

    public Duration getKeepAliveDuration() {
        return keepAliveDuration;
    }

}
