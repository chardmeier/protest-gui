#! /bin/bash

javac -Xlint:deprecation -classpath lib/flyingsaucer/core-renderer.jar:lib/sqlite-jdbc-3.8.11.2.jar:lib/jdbcdslog-1.0.5.jar:lib/slf4j-api-1.7.17.jar:. protest/*.java
