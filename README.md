# event-scheduler

Add this java library to your project to generate timed events.

For instance during a performance test to dynamically increase response times over time.

# usage

Create a an EventScheduler using the builders:

```java
EventSchedulerSettings settings = new EventSchedulerSettingsBuilder()
        .setKeepAliveInterval(Duration.ofMinutes(2))
        .build();

TestContext context = new TestContextBuilder()
        .setTestType("testType")
        .setTestEnvironment("testEnv")
        .setTestRunId("testRunId")
        .setCIBuildResultsUrl("http://url")
        .setApplicationRelease("release")
        .setRampupTimeInSeconds("10")
        .setConstantLoadTimeInSeconds("300")
        .setAnnotations("annotation")
        .setVariables(Collections.emptyMap())
        .setTags("tag1,tag2")
        .build();

Properties properties = new Properties();
properties.put("name", "value");
properties.put(EventProperties.FACTORY_CLASSNAME_KEY, "nl.stokpop.eventscheduler.event.EventFactoryDefault");

EventScheduler scheduler = new EventSchedulerBuilder()
        .setEventSchedulerSettings(settings)
        .setTestContext(context)
        .setAssertResultsEnabled(true)
        .addEvent("myEvent1", properties)
        .addEvent("myEvent2", properties)
        .addEvent("myEvent3", properties)
        .setCustomEvents(eventSchedule)
        .setLogger(testLogger)
        .setEventFactoryProvider(provider)
        .build();
```

Note that a lot of properties of the builders have decent defaults 
and do not need to be called, such as the retry and keep alive properties.

When adding events, each event can have its own properties. That makes it possible
to configure multiple Wiremock event that use different Wiremock urls for instance. 

The `addEvent` accepts a unique event name and the properties for that particular event.
Also, the event factory class needs to be provided via the `EventProperties.FACTORY_CLASSNAME_KEY`, 
so the events can be dynamically instantiated based on the availability on the classpath
of the `EventFactory` and `Event` implementation classes.
 
Then call these methods at the appropriate time:

### scheduler.beforeTest()
Call when the load test starts. 

### scheduler.afterTest()
Call when the load test stops. 

## Test Events

During a test run this Event Scheduler emits events. You can put
your own implementation of the `EventFactory` and `Event` interface on the classpath
and add your own code to these events.

Events triggers available, with example usage:
* _before test_ - use to restart servers or setup/cleanup environment
* _after test_ - start generating reports, clean up environment
* _keep alive calls_ - send calls to any remote API for instance
* _check result_ - after a test check results for the event, if failures are present the CI build can fail 
* _abort test_ - abort a running test, do not run to end
* _custom events_ - any event you can define in the event scheduler, e.g. failover, increase stub delay times or scale-down events 
* _custom events_ - any event you can define in the event scheduler, e.g. failover, increase stub delay times or scale-down events 

The keep alive is scheduled each 15 seconds during the test. The keep-alive schedule can also be changed.
	
## custom events

You can provide custom events via a list of 

    <duration,eventName(description),eventSettings> 

tuples, one per line.

The eventName can be any unique name among the custom events. You can use this eventName
in your own implementation of the Event interface to select what code to execute.

The description can be any text to explain what the event at that time is about. It will
be sent to remote systems and is for instance shown in the graphs as an event marker. 
If no description is provided, the description is 'eventName-duration'.

You can even send some specific settings to the event, using the eventSettings String.
Decide for your self if you want this to be just one value, a list of key-value pairs, 
json snippet or event base64 encoded contents.

Example:

```java
builder.setEventSchedule(eventSchedule)
```    
And as input:

```java
String eventSchedule =
        "PT5S|restart(restart with 2 replicas)|{ server:'myserver' replicas:2 tags: [ 'first', 'second' ] }\n" +
        "PT10M|scale-down\n" +
        "PT10M45S|heapdump(generate heapdump on port 1567)|server=myserver.example.com;port=1567\n" +
        "PT15M|scale-up|{ replicas:2 }\n";
```

Note the usage of ISO-8601 duration or period format, defined as PT(n)H(n)M(n)S.
Each period is from the start of the test, so not between events!

Above can be read as: 
* send restart event 5 seconds after start of test run. 
* send scale-down event 10 minutes after start of the test run.
* send heapdump event 10 minutes and 45 seconds after start of test run.
* send scale-up event 15 minutes after start of test run.

The setting will be send along with the event as well, for your own code to interpret.

When no settings are present, like with de scale-down event in this example, the settings
event will receive null for settings.

## Use of events in maven plugin

To use the events via the Gatling maven plugin the jar with the
implementation details must be on the classpath of the plugin.

You can use the `dependencies` element inside the `plugin` element.

For example:

```xml 
<plugin>
    <groupId>nl.stokpop</groupId>
    <artifactId>gatling-maven-plugin-events</artifactId>
    <configuration>
        <simulationClass>afterburner.AfterburnerBasicSimulation</simulationClass>
        <eventScheduleScript>
            PT5S|restart|{ server:'myserver' replicas:2 tags: [ 'first', 'second' ] }
            PT10M|scale-down
            PT10M45S|heapdump|server=myserver.example.com;port=1567
            PT15M|scale-up|{ replicas:2 }
        </eventScheduleScript>
        <eventProperties>
            <StokpopHelloEvent1>
                <eventFactory>nl.stokpop.event.StokpopHelloEventFactory</eventFactory>
                <myRestServer>https://my-rest-api</myName>
                <myCredentials>${ENV.SECRET}</myCredentials>
            </StokpopHelloEvent1>
        </eventProperties>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>nl.stokpop</groupId>
            <artifactId>hello-world-events</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</plugin>
```

Note that the `eventFactory` element is mandatory, it defines the factory to use to create an event.
The `StokpopHelloEvent1` element is the name of the particular event created by the Factory and can be 
any unique event name. The event name is used in the logging. 

# custom event schedule generator

To create your own event schedule you can implement your own
`nl.stokpop.eventscheduler.api.EventGenerator` and `EventGeneratorFactory`.

And add the following generator-class and settings to the customEvents tag
of the Gatling or jMeter plugin (instead of a verbatim list of events).

```xml
<customEvents>
    @generatorFactoryClass=com.stokpop.event.StokpopEventGenerator
    events-file=${project.basedir}/src/test/resources/events.json
    foo=bar
</customEvents>
```

The class defined by `@generatorFactoryClass` should be available on the classpath.
The `foo=bar` is an example of properties for the event generator.
You can use multiple lines for multiple properties.

Properties that start with @-sign are so-called "meta" properties and
should generally not be used as properties inside the implementation class.   

## ClassLoaders
If classes are not available on the default classpath of the Thread, you can provide your
own ClassLoader via `nl.stokpop.eventscheduler.api.EventSchedulerBuilder.build(java.lang.ClassLoader)`.
Useful when running with Gradle instead of Maven.