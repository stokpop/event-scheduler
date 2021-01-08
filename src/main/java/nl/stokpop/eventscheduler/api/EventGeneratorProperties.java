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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Properties for event schedule generators.
 * Names that start with @ are filtered out and used as meta properties.
 * Meta property lines have syntax <code>@metaproperty=value</code>.
 *
 * This is an immutable class and makes an unmodifiable copies of the given Map.
 */
public class EventGeneratorProperties {

    private static final Pattern PATTERN_NEW_LINE = Pattern.compile("\n");
    private static final String PREFIX_META_PROPERTY = "@";

    private Map<String, String> properties;
    private Map<String, String> metaProperties;

    public EventGeneratorProperties(Map<String,String> props) {

        Map<String, String> propsMap = props.entrySet().stream()
                .filter(e -> !e.getKey().startsWith(PREFIX_META_PROPERTY))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // get metadata from lines starting with @ and remove @ from the @key=value entry
        Map<String, String> metaMap = props.entrySet().stream()
                .filter(e -> e.getKey().startsWith(PREFIX_META_PROPERTY))
                .collect(Collectors.toMap(e -> e.getKey().substring(PREFIX_META_PROPERTY.length()), Map.Entry::getValue));

        Set<String> metaOptions = metaMap.keySet();
        if (!metaOptions.stream()
                .allMatch(EventGeneratorMetaProperty::isEnumValue)) {
            String availableOptions = Arrays.stream(EventGeneratorMetaProperty.values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new EventSchedulerRuntimeException(String.format("Contains unknown meta option: %s, availabe: %s", metaOptions, availableOptions));
        }

        properties = Collections.unmodifiableMap(propsMap);
        metaProperties = Collections.unmodifiableMap(metaMap);
    }

    public EventGeneratorProperties() {
        properties = Collections.emptyMap();
    }

    public EventGeneratorProperties(String propsAsText) {
        this(Collections.unmodifiableMap(createGeneratorSettings(propsAsText)));
    }

    public String getProperty(String name) {
        return properties.get(name);
    }

    public String getMetaProperty(String name) {
        return metaProperties.get(name);
    }

    private static Map<String, String> createGeneratorSettings(String generatorSettingsAsText) {
        return PATTERN_NEW_LINE.splitAsStream(generatorSettingsAsText)
                .map(line -> line.split("="))
                .filter(split -> split.length == 2)
                .filter(split -> split[0] != null && split[1] != null)
                .collect(Collectors.toMap(e -> e[0].trim(), e -> e[1].trim()));
    }

    public static boolean hasLinesThatStartWithMetaPropertyPrefix(String text) {
        return PATTERN_NEW_LINE.splitAsStream(text)
                .map(String::trim)
                .anyMatch(line -> line.startsWith(PREFIX_META_PROPERTY));
    }

    @Override
    public String toString() {
        return "GeneratorProperties{" + "properties=" + properties +
                ", metaProperties=" + metaProperties +
                '}';
    }
}
