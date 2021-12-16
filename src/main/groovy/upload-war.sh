#!/bin/sh

sshpass -p "l@s)-(@R!12345" ssh shussain@40.114.51.80 ./deploy-war.sh $JAR_NAME
echo "Exiting..."