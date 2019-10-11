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
package nl.stokpop.eventscheduler.generator;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Properties for event schedule generators.
 * Names that start with @ are filtered out.
 *
 *  This is an immutable class and makes an unmodifiable copies of the given Map.
 */
public class EventGeneratorProperties {
    private Map<String, String> properties;
    private Map<String, String> metaProperties;

    public EventGeneratorProperties(Map<String,String> props) {

        Map<String, String> propsMap = props.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("@"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, String> metaMap = props.entrySet().stream()
                .filter(e -> e.getKey().startsWith("@"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        properties = Collections.unmodifiableMap(propsMap);
        metaProperties = Collections.unmodifiableMap(metaMap);
    }

    public EventGeneratorProperties() {
        properties = Collections.unmodifiableMap(Collections.emptyMap());
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
        return Stream.of(generatorSettingsAsText.split("\n"))
                .map(line -> line.split("="))
                .filter(split -> split.length == 2)
                .filter(split -> split[0] != null && split[1] != null)
                .collect(Collectors.toMap(e -> e[0].trim(), e -> e[1].trim()));
    }

    @Override
    public String toString() {
        return "GeneratorProperties{" + "properties=" + properties +
                ", metaProperties=" + metaProperties +
                '}';
    }
}
