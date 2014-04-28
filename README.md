OohLaLog Log4j Appender
=======================

Log 4j Appender for Oohlalog Cloud Logging Service will post log messages the OohLaLog using standard Log4J interfaces and configuration.

**Dependencies**

To use the OohLaLog Log4J Appender please include the following jars in your classpath:

*OohLaLog Jar*
```
oohlalog_log4j-0.1.1.jar 
```

Repository Info:

[http://nexus.bertramlabs.com/content/repositories/publicReleases/](http://nexus.bertramlabs.com/content/repositories/publicReleases/)

Maven info:
```
<dependency>
  <groupId>oohlalog</groupId>
  <artifactId>oohlalog-4j</artifactId>
  <version>0.1.1</version>
</dependency>
```

*GSON Jar*
```
gson-2.2.4.jar
```
Repository Info:

Maven Central

Maven info:
```
<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.2.4</version>
</dependency>
```


**Counters**
The OohLaLog log4j adapter also implements a new log level called "COUNT". This level can be used to create manual counters for the configured OohLaLog instance.

```
import com.oohlalog.log4j.CountLevel;

...

Logger logger = Logger.getLogger(MyClass.class.getName());
		 
logger.log(CountLevel.COUNT,"My Custom Counter"); // Message will become the name of the counter
```

The counter will appear on your OohLaLog [dashboard](http://bertram.d.pr/wVgU).
