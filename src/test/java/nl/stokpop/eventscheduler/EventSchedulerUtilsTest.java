package nl.stokpop.eventscheduler;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class EventSchedulerUtilsTest {

    @Test
    public void splitAndTrim() {
        assertEquals(Collections.singletonList("one"), EventSchedulerUtils.splitAndTrim(" one ", ","));
        assertEquals(Arrays.asList("one", "two"), EventSchedulerUtils.splitAndTrim(" one,   two ", ","));
        assertEquals(Arrays.asList("o", ",", "t"), EventSchedulerUtils.splitAndTrim(" o , t", ""));
        assertEquals(Collections.emptyList(), EventSchedulerUtils.splitAndTrim(null, ","));
        assertEquals(Collections.emptyList(), EventSchedulerUtils.splitAndTrim(null, ""));
        assertEquals(Collections.emptyList(), EventSchedulerUtils.splitAndTrim(null, null));
    }
}