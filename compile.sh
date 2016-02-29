#! /bin/bash

javac -Xlint:unchecked -Xlint:deprecation -classpath lib/flyingsaucer/core-renderer.jar:lib/sqlite-jdbc-3.8.11.2.jar:lib/jdbcdslog-1.0.5.jar:lib/slf4j-api-1.7.17.jar:lib/c3p0-0.9.5.2.jar:lib/mchange-commons-java-0.2.11.jar:. protest/*.java
