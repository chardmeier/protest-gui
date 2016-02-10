#! /bin/bash

javac -Xlint:deprecation -classpath lib/flyingsaucer/core-renderer.jar:lib/sqlite-jdbc-3.8.11.2.jar:. protest/*.java
