#!/bin/zsh
cd app/Consumer
mvn clean package
java -jar target/Consumer-1.0-SNAPSHOT-jar-with-dependencies.jar target/classes/rabbitmq.conf