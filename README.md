# event-scheduler

Add this java library to your project to easily generate events.

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
        .setTags("")
        .build();

EventScheduler scheduler = new EventSchedulerBuilder()
    .setEventSchedulerSettings(settings)
    .setTestContext(context)
    .setAssertResultsEnabled(true)
    .addEventProperty("myClass", "name", "value")
    .setCustomEvents(eventSchedule)
    .setLogger(testLogger)
    .build();
```

Note that a lot of properties have decent defaults and do not need to be 
called, such as the retry and keep alive properties.

Then call these methods at the appropriate time:

### scheduler.beforeTest()
Call when the load test starts. 

### scheduler.afterTest()
Call when the load test stops. 

## Test Events

During a test run this Event Scheduler emits events. You can put
your own implementation of the `Event` interface on the classpath
and add your own code to these events.

Events available, with example usage:
* _before test_ - use to restart servers or setup/cleanup environment
* _after test_ - start generating reports, clean up environment
* _keep alive calls_ - send calls to any remote API for instance
* _custom events_ - any event you can define in the event scheduler, e.g. failover, increase stub delay times or scale-down events 
* _custom events_ - any event you can define in the event scheduler, e.g. failover, increase stub delay times or scale-down events 

The keep alive is scheduled each 15 seconds during the test.

The events will also be given a set of properties per implementation class.
The properties can be added before the test run using the `EventSchedulerBuilder`.

To add a property, use the addEventProperty when creating the client:

```java
builder.addEventProperty("nl.stokpop.MyEventClass", "name", "value")
```
	
## custom events

You can provide custom events via a list of <duration,eventName(description),eventSettings> tuples, 
one on each line.

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
            <nl.stokpop.event.StokpopHelloEvent>
                <myRestServer>https://my-rest-api</myName>
                <myCredentials>${ENV.SECRET}</myCredentials>
            </nl.stokpop.event.StokpopHelloEvent>
        </eventProperties>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>nl.stokpop</groupId>
            <artifactId>hello-world-events</artifactId>
            <version>1.2.3</version>
        </dependency>
    </dependencies>
</plugin>
```

# custom event schedule generator

To create your own event schedule you can implement your own
`nl.stokpop.eventscheduler.event.EventGenerator`.

And add the following generator-class and settings to the customEvents tag
of the gatling or jmeter plugin (instead of a verbatim list of events).

```xml
<customEvents>
    @generator-class=com.stokpop.event.StokpopEventGenerator
    events-file=${project.basedir}/src/test/resources/events.json
    foo=bar
</customEvents>
```

The generator-class should be available on the classpath.
The foo=bar is an example of properties for the event generator.
You can use multiple lines.

Properties that start with @-sign are so-called "meta" properties and
should generally not be used as properties inside the implementation class.   

## ClassLoaders
If classes are not available on the default classpath of the Thread, you can provide your
own ClassLoader via `nl.stokpop.eventscheduler.EventSchedulerBuilder.build(java.lang.ClassLoader)`.
