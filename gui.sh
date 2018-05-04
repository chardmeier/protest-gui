#! /bin/bash

java -Dorg.slf4j.simpleLogger.defaultLogLevel=error -classpath lib/flyingsaucer/core-renderer.jar:lib/sqlite-jdbc-3.21.0.jar:lib/jdbcdslog-1.0.5.jar:lib/slf4j-api-1.7.17.jar:lib/slf4j-simple-1.7.17.jar:lib/c3p0-0.9.5.2.jar:lib/mchange-commons-java-0.2.11.jar:. protest.ProtestGUI
