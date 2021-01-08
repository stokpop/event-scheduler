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

import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;
import org.junit.Test;

import static org.junit.Assert.*;

public class CustomEventTest {

    @Test(expected = CustomEvent.ScheduleEventWrongFormat.class)
    public void createFromLineNull() {
        CustomEvent.createFromLine(null);
    }

    @Test(expected = CustomEvent.ScheduleEventWrongFormat.class)
    public void createFromLineEmpty() {
        CustomEvent.createFromLine(" ");
    }

    @Test(expected = CustomEvent.ScheduleEventWrongFormat.class)
    public void createFromLineDurationOnly() {
        CustomEvent.createFromLine("  PT0S ");
    }

    @Test(expected = CustomEvent.ScheduleEventWrongFormat.class)
    public void createFromLineInvalidDuration() {
        CustomEvent.createFromLine("PT0X|name");
    }

    @Test(expected = CustomEvent.ScheduleEventWrongFormat.class)
    public void createFromLineInvalidDurationEmpty() {
        CustomEvent.createFromLine(" |name");
    }

    @Test(expected = CustomEvent.ScheduleEventWrongFormat.class)
    public void createFromLineInvalidTooMany() {
        CustomEvent.createFromLine("z|name|y|x");
    }

    @Test
    public void createFromLineWithSpaces() {
        CustomEvent event = CustomEvent.createFromLine("  PT13S|eventname( )|settings  =  0; foo= bar\n");

        assertEquals("eventname", event.getName());
        assertEquals(13, event.getDuration().getSeconds());
        assertEquals("eventname-PT13S", event.getDescription());
        assertEquals("settings  =  0; foo= bar", event.getSettings());
    }

    @Test
    public void createFromLineWithNoSettings() {
        CustomEvent event = CustomEvent.createFromLine("PT13S|eventname\n");

        assertEquals("eventname", event.getName());
        assertEquals(13, event.getDuration().getSeconds());
        assertEquals("eventname-PT13S", event.getDescription());
        assertNull(event.getSettings());
    }

    @Test
    public void createFromLineWithEventDescription() {
        CustomEvent event = CustomEvent.createFromLine("PT13S|eventname(Nice description of event)\n");

        assertEquals("eventname", event.getName());
        assertEquals(13, event.getDuration().getSeconds());
        assertEquals("Nice description of event", event.getDescription());
        assertNull(event.getSettings());
    }

    @Test
    public void createFromLineNastyCharacters() {
        String nastyJavascript = "javascript: alert(document.cookie);";
        CustomEvent event = CustomEvent.createFromLine("  PT13S|eventname(" + nastyJavascript + ")|settings  =  0; foo= bar\n");

        assertEquals("eventname", event.getName());
        assertEquals(13, event.getDuration().getSeconds());
        assertNotEquals(nastyJavascript, event.getDescription());
        assertEquals("settings  =  0; foo= bar", event.getSettings());
    }

    @Test
    public void extractNameAndDescription1() {
        String[] nameAndDescription = CustomEvent.extractNameAndDescription("");
        assertEquals(2, nameAndDescription.length);
        assertEquals("", nameAndDescription[0]);
        assertEquals("", nameAndDescription[1]);
    }

    @Test
    public void extractNameAndDescription2() {
        String[] nameAndDescription = CustomEvent.extractNameAndDescription("my-name(my-description)");
        assertEquals(2, nameAndDescription.length);
        assertEquals("my-name", nameAndDescription[0]);
        assertEquals("my-description", nameAndDescription[1]);
    }

    @Test
    public void extractNameAndDescription3() {
        String[] nameAndDescription = CustomEvent.extractNameAndDescription("   my-name    (  my-description = +100%   )   ");
        assertEquals(2, nameAndDescription.length);
        assertEquals("my-name", nameAndDescription[0]);
        assertEquals("my-description = +100%", nameAndDescription[1]);
    }

    @Test
    public void extractNameAndDescription4() {
        String[] nameAndDescription = CustomEvent.extractNameAndDescription("      (    )   ");
        assertEquals(2, nameAndDescription.length);
        assertEquals("", nameAndDescription[0]);
        assertEquals("", nameAndDescription[1]);
    }

    @Test
    public void extractNameAndDescription5() {
        String nastyJavascript = "javascript: alert(document.cookie);";
        String[] nameAndDescription = CustomEvent.extractNameAndDescription("  nastyJavascript     (  " + nastyJavascript + "  )   ");
        assertEquals(2, nameAndDescription.length);
        assertEquals("nastyJavascript", nameAndDescription[0]);
        assertNotEquals(nastyJavascript, nameAndDescription[1]);
    }

    @Test(expected = EventSchedulerRuntimeException.class)
    public void extractNameAndDescription6() {
        CustomEvent.extractNameAndDescription("my-name( x  ");
    }


}