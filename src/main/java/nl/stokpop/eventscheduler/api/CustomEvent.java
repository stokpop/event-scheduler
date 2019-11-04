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
package nl.stokpop.eventscheduler.api;

import nl.stokpop.eventscheduler.exception.EventSchedulerRuntimeException;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static nl.stokpop.eventscheduler.EventSchedulerUtils.hasValue;

public class CustomEvent {

    private static final Pattern nonAlphaNumsPattern = Pattern.compile("[^A-Za-z0-9\\- %+=:]");
    
    private Duration duration;
    private String name;
    private String description;
    private String settings;

    public CustomEvent(Duration duration, String name, String description, String settings) {
        this.duration = duration;
        this.name = name;
        this.description = hasValue(description) ? description : name + "-" + duration.toString();
        this.settings = settings;
    }

    public CustomEvent(Duration duration, String name, String description) {
        this(duration, name, description, null);
    }

    public Duration getDuration() {
        return duration;
    }

    public String getName() {
        return name;
    }

    public String getNameDescription() {
        return hasValue(description)
                ? "(" + description + ")"
                : "";
    }

    public String getSettings() {
        return settings;
    }

    /**
     * Use this format: duration|event-name(description)|settings
     *
     * Note: description and settings are optional. duration is in ISO-8601 format.
     *
     * The duration is the time from the start of the test until the event to fire.
     *
     * Examples:
     * <ul>
     *     <li>PT1M|change-backend-delay|delay=PT2S</li>
     *     <li>PT5M|change-backend-delay(set to extreme delay to test timeouts)|delay=PT10M</li>
     * </ul>
     *
     * @param line line that is separated by duration|event-name(description)|settings
     * @return new ScheduleEvent
     */
    public static CustomEvent createFromLine(String line) {

        if (line == null || line.trim().isEmpty()) {
            throw new ScheduleEventWrongFormat("empty line: [" + line + "]");
        }

        if (!line.contains("|")) {
            throw new ScheduleEventWrongFormat("line should contain at least a duration and event name, separated by '|': [" + line + "]");
        }

        List<String> elements = Arrays.stream(line.split("\\|"))
                .map(String::trim)
                .collect(Collectors.toList());

        if (!(elements.size() == 2 || elements.size() == 3)) {
            throw new ScheduleEventWrongFormat("Wrong number of elements in line, expected 'duration|name(description)|setting' " +
                    "where (description) and settings are optional: [" + line + "]");
        }
        
        Duration duration;
        String textDuration = elements.get(0);
        String nameWithDescription = elements.get(1).trim();

        String[] nameAndDescriptionPair = extractNameAndDescription(nameWithDescription);
        String name = nameAndDescriptionPair[0];
        String description = nameAndDescriptionPair[1];

        try {
            duration = Duration.parse(textDuration);
        } catch (Exception e) {
            throw new ScheduleEventWrongFormat("Failed to parse duration: [" + textDuration + "] from line: [" + line + "]", e);
        }
        
        if (elements.size() == 2) {
            return new CustomEvent(duration, name, description);
        }
        else {
            String settings = elements.get(2);
            return new CustomEvent(duration, name, description, settings);
        }
    }

    /**
     * For name(description) return a String array of size 2 with { "name", "description" }
     *
     * For name returns { "name", "" }
     *
     * For name() returns { "name", "" }
     *
     * For (description) returns { "", "" }
     *
     * Also trims all values. And replaces all non alpha-numeric (and '+','-',' ','%') characters with _.
     *
     * @return array of size two with name and description (possibly empty string when not present)
     */
     static String[] extractNameAndDescription(String nameWithDescription) {
        if (!hasValue(nameWithDescription)) return new String[] { "" , "" };
        if (!nameWithDescription.contains("(")) {
            return new String[] { nameWithDescription, "" };
        }
        int indexOpen = nameWithDescription.indexOf("(");
        int indexClose = nameWithDescription.lastIndexOf(")");
        if (indexClose == -1) { throw new EventSchedulerRuntimeException("closing parentheses ')' is missing in '" + nameWithDescription + "'"); }

        String name = nameWithDescription.substring(0, indexOpen).trim();
        String description = nameWithDescription.substring(indexOpen + 1, indexClose).trim();

        String sanitizedName = nonAlphaNumsPattern.matcher(name).replaceAll("_");
        String sanitizedDescription = nonAlphaNumsPattern.matcher(description).replaceAll("_");
        
        return new String[] { sanitizedName, sanitizedDescription };
    }

    @Override
    public String toString() {
         String formattedDesc = getNameDescription();

         return settings == null
                ? String.format("ScheduleEvent %s%s [fire-at=%s]", name, formattedDesc, duration)
                : String.format("ScheduleEvent %s%s [fire-at=%s settings=%s]", name, formattedDesc, duration, limitString(settings, 50));
    }

    private String limitString(String text, int maxLength) {
         if (text.length() <= maxLength) {
             return text;
         }
         else {
             return text.substring(0, maxLength) + "...";
         }
    }

    public String getDescription() {
        return description;
    }

    public static class ScheduleEventWrongFormat extends RuntimeException {
        public ScheduleEventWrongFormat(String message) {
            super(message);
        }

        public ScheduleEventWrongFormat(String message, Exception e) {
           super(message, e);
        }
    }
}
