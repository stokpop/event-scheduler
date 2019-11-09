package nl.stokpop.eventscheduler.api;

public class EventCheck {

    public static final EventCheck DEFAULT =
            new EventCheck("default", EventCheck.class.getName(), EventStatus.UNKNOWN,
            "This is the default EventCheck, make sure to implement if needed!");

    private final String eventId;
    private final String eventClassName;
    private final EventStatus eventStatus;
    private final String message;

    public EventCheck(String eventId, String eventClassName, EventStatus eventStatus, String message) {
        this.eventId = eventId;
        this.eventClassName = eventClassName;
        this.eventStatus = eventStatus;
        this.message = message;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventClassName() {
        return eventClassName;
    }

    public EventStatus getEventStatus() {
        return eventStatus;
    }

    public String getMessage() {
        return message;
    }
}
