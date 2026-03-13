#!/bin/bash

hsqldb_home=./hsqldb
rc_file=auth.rc
urlid=Hagi
sql_file=db.sql

# Start HSQLDB on port 9005 (avoids conflicts with Jupyter on 9001 and others)
java -cp "$hsqldb_home/lib/hsqldb.jar" org.hsqldb.server.Server \
    --port 9005 \
    --database.0 file:mydb \
    --dbname.0 xdb
