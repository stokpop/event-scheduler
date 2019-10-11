package nl.stokpop.eventscheduler.api;

import nl.stokpop.eventscheduler.EventSchedulerUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class TestContextBuilder {
    private static final int DEFAULT_RAMPUP_TIME_SECONDS = 0;
    private static final int DEFAULT_CONSTANT_LOAD_TIME_SECONDS = 600;

    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH-mm-ss");

    private String annotations = "";
    private String application = "unknown";
    private String testType = "unknown";
    private String testEnvironment = "unknown";
    private String testRunId = "unknown_" + dateTimeFormatter.format(LocalDateTime.now());
    private String ciBuildResultsUrl = "unknown";
    private String applicationRelease = "unknown";
    private Duration rampupTime = Duration.ofSeconds(DEFAULT_RAMPUP_TIME_SECONDS);
    private Duration constantLoadTime = Duration.ofSeconds(DEFAULT_CONSTANT_LOAD_TIME_SECONDS);
    private Map<String, String> variables = Collections.emptyMap();
    private List<String> tags = Collections.emptyList();

    public TestContextBuilder setApplication(String application) {
        if (EventSchedulerUtils.hasValue(application)) {
            this.application = application;
        }
        return this;
    }

    public TestContextBuilder setTestType(String testType) {
        if (EventSchedulerUtils.hasValue(testType)) {
            this.testType = testType;
        }
        return this;
    }

    public TestContextBuilder setTestEnvironment(String testEnvironment) {
        if (EventSchedulerUtils.hasValue(testEnvironment)) {
            this.testEnvironment = testEnvironment;
        }
        return this;
    }

    public TestContextBuilder setTestRunId(String testRunId) {
        if (EventSchedulerUtils.hasValue(testRunId)) {
            this.testRunId = testRunId;
        }
        return this;
    }

    public TestContextBuilder setCIBuildResultsUrl(String ciBuildResultsUrl) {
        if (EventSchedulerUtils.hasValue(ciBuildResultsUrl)) {
            this.ciBuildResultsUrl = ciBuildResultsUrl;
        }
        return this;
    }

    public TestContextBuilder setApplicationRelease(String applicationRelease) {
        if (EventSchedulerUtils.hasValue(applicationRelease)) {
            this.applicationRelease = applicationRelease;
        }
        return this;
    }

    public TestContextBuilder setRampupTime(Duration rampupTime) {
        if (rampupTime != null) {
            this.rampupTime = rampupTime;
        }
        return this;
    }

    public TestContextBuilder setConstantLoadTime(Duration constantLoadTime) {
        if (constantLoadTime != null) {
            this.constantLoadTime = constantLoadTime;
        }
        return this;
    }

    public TestContextBuilder setAnnotations(String annotations) {
        if (EventSchedulerUtils.hasValue(annotations)) {
            this.annotations = annotations;
        }
        return this;
    }

    public TestContextBuilder setVariables(Map<String, String> variables) {
        if (variables != null) {
            this.variables = variables;
        }
        return this;
    }

    public TestContextBuilder setTags(List<String> tags) {
        if (tags != null) {
            this.tags = tags;
        }
        return this;
    }

    /**
     * @param tagsSeparatedByCommas example: foo,bar,zzz
     * @return this
     */
    public TestContextBuilder setTags(String tagsSeparatedByCommas) {
        this.tags = EventSchedulerUtils.splitAndTrim(tagsSeparatedByCommas,",");
        return this;
    }

    public TestContext build() {
        return new TestContext(application, testType, testEnvironment, testRunId, ciBuildResultsUrl, applicationRelease, rampupTime, constantLoadTime, annotations, variables, tags);
    }

    public TestContextBuilder setRampupTimeInSeconds(String rampupTimeSeconds) {
        this.rampupTime = Duration.ofSeconds(EventSchedulerUtils.parseInt("rampupTimeSeconds", rampupTimeSeconds, DEFAULT_RAMPUP_TIME_SECONDS));
        return this;
    }

    public TestContextBuilder setConstantLoadTimeInSeconds(String constantLoadTimeInSeconds) {
        this.constantLoadTime = Duration.ofSeconds(EventSchedulerUtils.parseInt("constantLoadTimeInSeconds", constantLoadTimeInSeconds, DEFAULT_CONSTANT_LOAD_TIME_SECONDS));
        return this;
    }

    public TestContextBuilder setVariables(Properties props) {
        if (props != null) {
            Map<String, String> vars = props.entrySet().stream().collect(
                    Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> e.getValue().toString()
                    )
            );
            this.setVariables(vars);
        }
        return this;
    }


}