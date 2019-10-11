/*
 * Copyright (C) 2019 Peter Paul Bakker, Stokpop Software Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.api.EventSchedulerLogger;
import nl.stokpop.eventscheduler.api.EventSchedulerSettings;
import nl.stokpop.eventscheduler.api.TestContext;
import nl.stokpop.eventscheduler.event.EventBroadcaster;
import nl.stokpop.eventscheduler.event.EventSchedulerProperties;
import nl.stokpop.eventscheduler.event.ScheduleEvent;
import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class EventSchedulerEngine {

    private final EventSchedulerLogger logger;

    private ScheduledExecutorService executorKeepAlive;
    private ScheduledExecutorService executorCustomEvents;

    EventSchedulerEngine(EventSchedulerLogger logger) {
        if (logger == null) {
            throw new EventSchedulerRuntimeException("logger is null");
        }
        this.logger = logger;
    }

    void startKeepAliveThread(TestContext context, EventSchedulerSettings settings, EventBroadcaster broadcaster, EventSchedulerProperties eventProperties) {
        nullChecks(context, broadcaster, eventProperties);

        if (executorKeepAlive != null) {
            throw new RuntimeException("cannot start keep alive thread multiple times!");
        }

        logger.info(String.format("calling keep alive every %s", settings.getKeepAliveDuration()));

        executorKeepAlive = createKeepAliveScheduler();
        
        KeepAliveRunner keepAliveRunner = new KeepAliveRunner(context, broadcaster, eventProperties);
        executorKeepAlive.scheduleAtFixedRate(keepAliveRunner, 0, settings.getKeepAliveDuration().getSeconds(), TimeUnit.SECONDS);
    }

    private void nullChecks(TestContext context, EventBroadcaster broadcaster, EventSchedulerProperties eventProperties) {
        if (context == null) {
            throw new NullPointerException("TestContext cannot be null");
        }
        if (broadcaster == null) {
            throw new NullPointerException("EventBroadcaster cannot be null");
        }
        if (eventProperties == null) {
            throw new NullPointerException("EventSchedulerProperties cannot be null");
        }
    }

    private void addToExecutor(ScheduledExecutorService executorService, TestContext context, ScheduleEvent event, EventSchedulerProperties eventProperties, EventBroadcaster broadcaster) {
        executorService.schedule(new EventRunner(context, eventProperties, event, broadcaster), event.getDuration().getSeconds(), TimeUnit.SECONDS);
    }

    void shutdownThreadsNow() {
        logger.info("shutdown Executor threads");
        if (executorKeepAlive != null) {
            executorKeepAlive.shutdownNow();
        }
        if (executorCustomEvents != null) {
            List<Runnable> runnables = executorCustomEvents.shutdownNow();
            if (runnables.size() > 0) {
                if (runnables.size() == 1) {
                    logger.warn("there is 1 custom event that is not (fully) executed!");
                }
                else {
                    logger.warn("there are " + runnables.size() + " custom events that are not (fully) executed!");
                }
            }
        }
        executorKeepAlive = null;
        executorCustomEvents = null;
    }

    void startCustomEventScheduler(TestContext context, List<ScheduleEvent> scheduleEvents, EventBroadcaster broadcaster, EventSchedulerProperties eventProperties) {
        nullChecks(context, broadcaster, eventProperties);

        if (!(scheduleEvents == null || scheduleEvents.isEmpty())) {

            logger.info(createEventScheduleMessage(scheduleEvents));

            executorCustomEvents = createCustomEventScheduler();
            scheduleEvents.forEach(event -> addToExecutor(executorCustomEvents, context, event, eventProperties, broadcaster));
        }
        else {
            logger.info("no custom schedule events found");
        }
    }

    public static String createEventScheduleMessage(List<ScheduleEvent> scheduleEvents) {
        StringBuilder message = new StringBuilder();
        message.append("=== custom events schedule ===");
        scheduleEvents.forEach(event -> message
                .append("\n==> ")
                .append(String.format("ScheduleEvent %-36.36s [fire-at=%-8s settings=%-50.50s]", event.getNameDescription(), event.getDuration(), event.getSettings())));
        return message.toString();
    }

    private ScheduledExecutorService createKeepAliveScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            String threadName = "Keep-Alive-Thread";
            logger.info("create new thread: " + threadName);
            return new Thread(r, threadName);
        });
    }

    private ScheduledExecutorService createCustomEventScheduler() {
        return Executors.newScheduledThreadPool(2, new ThreadFactory() {
            private final AtomicInteger threadCount = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                String threadName = "Custom-Event-Thread-" + threadCount.incrementAndGet();
                logger.info("create new thread: " + threadName);
                return new Thread(r, threadName);
            }
        });
    }
    
    class KeepAliveRunner implements Runnable {

        private final TestContext context;
        private final EventBroadcaster broadcaster;
        private final EventSchedulerProperties eventProperties;

        KeepAliveRunner(TestContext context, EventBroadcaster broadcaster, EventSchedulerProperties eventProperties) {
            this.context = context;
            this.broadcaster = broadcaster;
            this.eventProperties = eventProperties;
        }

        @Override
        public void run() {
            // TODO make recurring calls also part of the generic EventGenerators
            try {
                broadcaster.broadCastKeepAlive(context, eventProperties);
            } catch (Exception e) {
                logger.error("Broadcast keep-alive failed", e);
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

        public EventRunner(TestContext context, EventSchedulerProperties eventProperties, ScheduleEvent event, EventBroadcaster eventBroadcaster) {
            this.event = event;
            this.context = context;
            this.eventProperties = eventProperties;
            this.eventBroadcaster = eventBroadcaster;
        }

        @Override
        public void run() {
            try {
                eventBroadcaster.broadcastCustomEvent(context, eventProperties, event);
            } catch (Exception e) {
                logger.error("Broadcast custom event failed", e);
            }
        }

        @Override
        public String toString() {
            return String.format("EventRunner for event %s for testId %s", event, context.getTestRunId());
        }
    }

}
