package nl.stokpop.eventscheduler.event;

import nl.stokpop.eventscheduler.api.TestContext;

public interface EventBroadcaster {

    void broadcastBeforeTest(TestContext context, EventSchedulerProperties eventProperties);

    void broadcastAfterTest(TestContext context, EventSchedulerProperties eventProperties);

    void broadCastKeepAlive(TestContext context, EventSchedulerProperties eventProperties);

    void broadcastCustomEvent(TestContext context, EventSchedulerProperties eventProperties, ScheduleEvent event);
}
