## Logging
This document talks about structured logging and the Elastic stack

## Components
The following are various components used in logging: Elasticsearch Kibana & Filebeat

### Filebeat
[Filebeat](https://www.elastic.co/beats/filebeat) is a lightweight shipper for logs.  Helps in transporting the log files from application to Elasticsearch.  We use the following filebeat configuration

```yaml
filebeat.inputs:
  - type: log
    enabled: true
    json.message_key: message
    json.keys_under_root: true
    json.add_error_key: true
    paths:
      - "/applogs/app*"
output.elasticsearch:
  hosts: ["elasticsearch:9200"]
``` 

We are configuring the logs to be in JSON format

### Elasticsearch
[Elasticsearch](https://www.elastic.co/) is a software for searching and analyzing your data in realtime.  We are storing our logs in elasticsearch database so that it can be useful for debugging purposes.

It can be accessed at [this url](http://localhost:9200/) 

### Kibana
[Kibana](https://www.elastic.co/kibana) lets you visualize your elasticsearch data.  It helps you build visuals and explore the data available in Elasticsearch.

## Structured Logging
The Consent Manager application is configured to publish JSON structured logs so that elasticsearch can index it with ease.  We use this [logback.xml](src/main/resources/logback.xml) file for configuring the structured logs.

```xml
    <appender name="file-appender"
              class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_LOCATION}</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        <rollingPolicy
                class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            rollover daily and when the file reaches 10 MegaBytes
            <fileNamePattern>logs/logs.%d{yyyy-MM-dd}.%i.log
            </fileNamePattern>
            <timeBasedFileNamingAndTriggeringPolicy
                    class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>10MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

``` 

We are using [LogstashEncoder](https://github.com/logstash/logstash-logback-encoder) for converting the log messages into JSON format.  The following is an example log entry.

```json
{
  "@timestamp": "2020-04-15T11:44:45.589+05:30",
  "@version": "1",
  "message": "No active profile set, falling back to default profiles: default",
  "logger_name": "com.example.demo.DemoApplication",
  "thread_name": "main",
  "level": "INFO",
  "level_value": 20000
}
```

### How to log entries?
Nothing special.  Just log it using standard [Slf4j](http://www.slf4j.org/) interface
```java
log.info("logging demo!");
```

It is possible to customize the logging and publish custom fields in the JSON.
```java
log.info("Audit",StructuredArguments.value("who", audit.getWho()), 
    StructuredArguments.value("app",audit.getApp()), StructuredArguments.v("operation", audit.getOperation()));
```
Adding custom entries using [StructuredArguments](https://github.com/logstash/logstash-logback-encoder/blob/master/src/main/java/net/logstash/logback/argument/StructuredArguments.java)  helps in adding custom entries to JSON file.  For more information, refer to this amazing blog [here](https://www.innoq.com/en/blog/structured-logging/)

The above code will generate a JSON like this
```json
{
  "@timestamp": "2020-04-15T11:44:49.762+05:30",
  "@version": "1",
  "message": "Audit",
  "logger_name": "com.example.demo.AuditService",
  "thread_name": "http-nio-8080-exec-3",
  "level": "INFO",
  "level_value": 20000,
  "who": "bob",
  "app": "consent-service",
  "operation": "USER_REGISTER"
}
```
Note that who, app & operation are custom fields added in log statement above.

### Configuring Kibana
Once filebeat starts publishing the messages to Elasticsearch, you can see a new index getting created with a name filebeat-7.6.2-xxxxxx.  You can configure this Index Pattern as mentioned [here](https://www.elastic.co/guide/en/kibana/current/tutorial-define-index.html)
Then you can start searching for the fields and interact with it in Kibana dashboard.