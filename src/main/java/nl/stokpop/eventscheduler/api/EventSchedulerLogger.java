package nl.stokpop.eventscheduler.api;

public interface EventSchedulerLogger {

    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable throwable);
    void debug(String message);

}
