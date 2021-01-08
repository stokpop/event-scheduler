/*
 * Copyright (C) 2021 Peter Paul Bakker, Stokpop Software Solutions
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
package nl.stokpop.eventscheduler.api;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventCheck that = (EventCheck) o;
        return Objects.equals(eventId, that.eventId) &&
                eventStatus == that.eventStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventStatus);
    }

    @Override
    public String toString() {
        return "EventCheck{" + "eventId='" + eventId + '\'' +
                ", eventClassName='" + eventClassName + '\'' +
                ", eventStatus=" + eventStatus +
                ", message='" + message + '\'' +
                '}';
    }
}
