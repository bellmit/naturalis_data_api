<?xml version="1.0" encoding="UTF-8"?>
<configuration>

	<timestamp key="date" datePattern="yyyyMMddHHmmss"/>

	<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
		<encoder>
			<pattern>%d %5p | %t | %-55logger{55} | %m %n</pattern>
		</encoder>
	</appender>

	<appender name="FILE" class="ch.qos.logback.core.FileAppender">
		<file>C:/test/nda-import/log/nda-import.${date}.log</file>
		<append>true</append>
		<encoder>
			<pattern>%d %5p | %t | %-55logger{55} | %m %n</pattern>
		</encoder>
	</appender>

	<root level="INFO">
		<appender-ref ref="FILE" />
		<appender-ref ref="CONSOLE" />
	</root>

</configuration>
