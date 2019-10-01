package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.api.PerfanaCaller;
import nl.stokpop.eventscheduler.api.PerfanaClientLogger;
import nl.stokpop.eventscheduler.api.PerfanaConnectionSettings;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.exception.PerfanaClientRuntimeException;
import nl.stokpop.eventscheduler.event.EventBroadcaster;
import nl.stokpop.eventscheduler.event.EventSchedulerProperties;
import nl.stokpop.eventscheduler.event.ScheduleEvent;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class EventSchedulerEngine {

    private final PerfanaClientLogger logger;

    private ScheduledExecutorService executorKeepAlive;
    private ScheduledExecutorService executorCustomEvents;

    EventSchedulerEngine(PerfanaClientLogger logger) {
        if (logger == null) {
            throw new PerfanaClientRuntimeException("logger is null");
        }
        this.logger = logger;
    }

    void startKeepAliveThread(PerfanaCaller perfana, TestContext context, PerfanaConnectionSettings settings, EventBroadcaster broadcaster, EventSchedulerProperties eventProperties) {
        nullChecks(perfana, context, broadcaster, eventProperties);

        if (executorKeepAlive != null) {
            throw new RuntimeException("cannot start keep alive thread multiple times!");
        }

        logger.info(String.format("calling Perfana (%s) keep alive every %s", settings.getPerfanaUrl(), settings.getKeepAliveDuration()));

        executorKeepAlive = createKeepAliveScheduler();
        
        KeepAliveRunner keepAliveRunner = new KeepAliveRunner(perfana, context, broadcaster, eventProperties);
        executorKeepAlive.scheduleAtFixedRate(keepAliveRunner, 0, settings.getKeepAliveDuration().getSeconds(), TimeUnit.SECONDS);
    }

    private void nullChecks(PerfanaCaller perfana, TestContext context, EventBroadcaster broadcaster, EventSchedulerProperties eventProperties) {
        if (perfana == null) {
            throw new NullPointerException("PerfanaCaller cannot be null");
        }
        if (context == null) {
            throw new NullPointerException("TestContext cannot be null");
        }
        if (broadcaster == null) {
            throw new NullPointerException("PerfanaEventBroadcaster cannot be null");
        }
        if (eventProperties == null) {
            throw new NullPointerException("PerfanaEventProperties cannot be null");
        }
    }

    private void addToExecutor(ScheduledExecutorService executorService, TestContext context, ScheduleEvent event, EventSchedulerProperties eventProperties, PerfanaCaller perfana, EventBroadcaster broadcaster) {
        executorService.schedule(new EventRunner(context, eventProperties, event, broadcaster, perfana), event.getDuration().getSeconds(), TimeUnit.SECONDS);
    }

    void shutdownThreadsNow() {
        logger.info("shutdown Perfana Executor threads");
        if (executorKeepAlive != null) {
            executorKeepAlive.shutdownNow();
        }
        if (executorCustomEvents != null) {
            List<Runnable> runnables = executorCustomEvents.shutdownNow();
            if (runnables.size() > 0) {
                if (runnables.size() == 1) {
                    logger.warn("there is 1 custom Perfana event that is not (fully) executed!");
                }
                else {
                    logger.warn("there are " + runnables.size() + " custom Perfana events that are not (fully) executed!");
                }
            }
        }
        executorKeepAlive = null;
        executorCustomEvents = null;
    }

    void startCustomEventScheduler(PerfanaCaller perfana, TestContext context, List<ScheduleEvent> scheduleEvents, EventBroadcaster broadcaster, EventSchedulerProperties eventProperties) {
        nullChecks(perfana, context, broadcaster, eventProperties);

        if (!(scheduleEvents == null || scheduleEvents.isEmpty())) {

            logger.info(createEventScheduleMessage(scheduleEvents));

            executorCustomEvents = createCustomEventScheduler();
            scheduleEvents.forEach(event -> addToExecutor(executorCustomEvents, context, event, eventProperties, perfana, broadcaster));
        }
        else {
            logger.info("no custom Perfana schedule events found");
        }
    }

    public static String createEventScheduleMessage(List<ScheduleEvent> scheduleEvents) {
        StringBuilder message = new StringBuilder();
        message.append("=== custom Perfana events schedule ===");
        scheduleEvents.forEach(event -> message
                .append("\n==> ")
                .append(String.format("ScheduleEvent %-36.36s [fire-at=%-8s settings=%-50.50s]", event.getNameDescription(), event.getDuration(), event.getSettings())));
        return message.toString();
    }

    private ScheduledExecutorService createKeepAliveScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            String threadName = "Perfana-Keep-Alive-Thread";
            logger.info("create new thread: " + threadName);
            return new Thread(r, threadName);
        });
    }

    private ScheduledExecutorService createCustomEventScheduler() {
        return Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private final AtomicInteger perfanaThreadCount = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                String threadName = "Perfana-Custom-Event-Thread-" + perfanaThreadCount.incrementAndGet();
                logger.info("create new thread: " + threadName);
                return new Thread(r, threadName);
            }
        });
    }
    
    class KeepAliveRunner implements Runnable {

        private final PerfanaCaller perfana;
        private final TestContext context;
        private final EventBroadcaster broadcaster;
        private final EventSchedulerProperties eventProperties;

        KeepAliveRunner(PerfanaCaller perfana, TestContext context, EventBroadcaster broadcaster, EventSchedulerProperties eventProperties) {
            this.perfana = perfana;
            this.context = context;
            this.broadcaster = broadcaster;
            this.eventProperties = eventProperties;
        }

        @Override
        public void run() {
            try {
                perfana.callPerfanaTestEndpoint(context, false);
            } catch (Exception e) {
                logger.error("Perfana call for keep-alive failed", e);
            }
            try {
                broadcaster.broadCastKeepAlive(context, eventProperties);
            } catch (Exception e) {
                logger.error("Perfana broadcast keep-alive failed", e);
            }

        }

        @Override
        public String toString() {
            return "KeepAliveRunner for " + context.getTestRunId();
        }
    }

    class EventRunner implements Runnable {

        private final ScheduleEvent event;

        private final TestContext context;
        private final EventSchedulerProperties eventProperties;

        private final EventBroadcaster eventBroadcaster;
        private final PerfanaCaller perfana;

        public EventRunner(TestContext context, EventSchedulerProperties eventProperties, ScheduleEvent event, EventBroadcaster eventBroadcaster, PerfanaCaller perfana) {
            this.event = event;
            this.context = context;
            this.eventProperties = eventProperties;
            this.eventBroadcaster = eventBroadcaster;
            this.perfana = perfana;
        }

        @Override
        public void run() {
            try {
                perfana.callPerfanaEvent(context, event.getDescription());
            } catch (Exception e) {
                logger.error("Perfana call event failed", e);
            }
            try {
                eventBroadcaster.broadcastCustomEvent(context, eventProperties, event);
            } catch (Exception e) {
                logger.error("Perfana broadcast event failed", e);
            }
        }

        @Override
        public String toString() {
            return String.format("EventRunner for event %s for testId %s", event, context.getTestRunId());
        }
    }

}
