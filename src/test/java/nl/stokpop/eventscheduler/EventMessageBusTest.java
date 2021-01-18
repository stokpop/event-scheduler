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
package nl.stokpop.eventscheduler;

import nl.stokpop.eventscheduler.api.message.EventMessage;
import nl.stokpop.eventscheduler.api.message.EventMessageBus;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class EventMessageBusTest {

    @Test
    public void sendMinimalCase() {
        EventMessage messageOne = EventMessage.builder().build();
        EventMessageBus eventMessageBus = new EventMessageBusSimple();
        eventMessageBus.send(messageOne);
    }

    @Test
    public void sendWithReceiver() {
        EventMessage messageOne = EventMessage.builder().build();
        AtomicInteger called = new AtomicInteger(0);

        EventMessageBus eventMessageBus = new EventMessageBusSimple();
        eventMessageBus.addReceiver(message -> { called.incrementAndGet(); assertEquals(messageOne, message); } );
        eventMessageBus.send(messageOne);

        assertEquals("receiver should be called once", 1, called.get());
    }
}