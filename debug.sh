#! /bin/bash

jdb -classpath lib/flyingsaucer/core-renderer-minimal.jar:lib/sqlite-jdbc-3.8.11.2.jar:. protest.ProtestGUI protestsuite.db
