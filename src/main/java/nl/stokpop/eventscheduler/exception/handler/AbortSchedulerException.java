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
package nl.stokpop.eventscheduler.exception.handler;

import nl.stokpop.eventscheduler.api.SchedulerExceptionType;

/**
 * An event can throw AbortSchedulerException to stop the
 * scheduler and trigger the registered abort handlers.
 *
 * Use in case of unrecoverable problems.
 */
public class AbortSchedulerException extends SchedulerHandlerException {
    public AbortSchedulerException(String message) {
        super(message);
    }

    @Override
    public SchedulerExceptionType getExceptionType() {
        return SchedulerExceptionType.ABORT;
    }

}
