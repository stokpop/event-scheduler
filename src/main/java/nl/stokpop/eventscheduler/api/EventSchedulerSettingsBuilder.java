package nl.stokpop.eventscheduler.api;

import nl.stokpop.eventscheduler.EventSchedulerUtils;

import java.time.Duration;

public class EventSchedulerSettingsBuilder {

    private static final int DEFAULT_KEEP_ALIVE_TIME_SECONDS = 30;
    private Duration keepAliveInterval = Duration.ofSeconds(DEFAULT_KEEP_ALIVE_TIME_SECONDS);

    public EventSchedulerSettingsBuilder setKeepAliveTimeInSeconds(String keepAliveTimeInSeconds) {
        this.keepAliveInterval = Duration.ofSeconds(EventSchedulerUtils.parseInt("keepAliveTimeInSeconds", keepAliveTimeInSeconds, DEFAULT_KEEP_ALIVE_TIME_SECONDS));
        return this;
    }

    public EventSchedulerSettingsBuilder setKeepAliveInterval(Duration keepAliveInterval) {
        if (keepAliveInterval != null) {
            this.keepAliveInterval = keepAliveInterval;
        }
        return this;
    }
    
    public EventSchedulerSettings build() {
        return new EventSchedulerSettings(keepAliveInterval);
    }

}