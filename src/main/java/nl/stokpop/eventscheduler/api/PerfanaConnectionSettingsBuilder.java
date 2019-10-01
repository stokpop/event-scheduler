package nl.stokpop.eventscheduler.api;

import nl.stokpop.eventscheduler.EventSchedulerUtils;

import java.time.Duration;

public class PerfanaConnectionSettingsBuilder {

    private static final int DEFAULT_KEEP_ALIVE_TIME_SECONDS = 30;
    private static final int DEFAULT_RETRY_TIME_SECONDS = 10;
    private static final int DEFAULT_RETRY_MAX_COUNT = 30;
    private String perfanaUrl = "unknown";
    private Duration keepAliveInterval = Duration.ofSeconds(DEFAULT_KEEP_ALIVE_TIME_SECONDS);
    private int retryMaxCount = DEFAULT_RETRY_MAX_COUNT;
    private Duration retryDuration = Duration.ofSeconds(DEFAULT_RETRY_TIME_SECONDS);

    public PerfanaConnectionSettingsBuilder setKeepAliveTimeInSeconds(String keepAliveTimeInSeconds) {
        this.keepAliveInterval = Duration.ofSeconds(EventSchedulerUtils.parseInt("keepAliveTimeInSeconds", keepAliveTimeInSeconds, DEFAULT_KEEP_ALIVE_TIME_SECONDS));
        return this;
    }

    public PerfanaConnectionSettingsBuilder setRetryMaxCount(String retryMaxCount) {
        this.retryMaxCount = EventSchedulerUtils.parseInt("retryMaxCount", retryMaxCount, DEFAULT_RETRY_MAX_COUNT);
        return this;
    }

    public PerfanaConnectionSettingsBuilder setRetryTimeInSeconds(String retryTimeInSeconds) {
        this.retryDuration = Duration.ofSeconds(EventSchedulerUtils.parseInt("retryTimeInSeconds", retryTimeInSeconds, DEFAULT_RETRY_TIME_SECONDS));
        return this;
    }

    public PerfanaConnectionSettingsBuilder setRetryMaxCount(int retryMaxCount) {
        this.retryMaxCount = retryMaxCount;
        return this;
    }

    public PerfanaConnectionSettingsBuilder setRetryDuration(Duration retryDuration) {
        if (retryDuration != null) {
            this.retryDuration = retryDuration;
        }
        return this;
    }

    public PerfanaConnectionSettingsBuilder setKeepAliveInterval(Duration keepAliveInterval) {
        if (keepAliveInterval != null) {
            this.keepAliveInterval = keepAliveInterval;
        }
        return this;
    }

    public PerfanaConnectionSettingsBuilder setPerfanaUrl(String perfanaUrl) {
        if (EventSchedulerUtils.hasValue(perfanaUrl)) {
            this.perfanaUrl = perfanaUrl;
        }
        return this;
    }

    public PerfanaConnectionSettings build() {
        return new PerfanaConnectionSettings(retryMaxCount, retryDuration, keepAliveInterval, perfanaUrl);
    }

}