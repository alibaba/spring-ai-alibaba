#!/bin/bash
cd docker/middleware/
docker compose up -d --build
sleep 5
cd ../../spring-ai-alibaba-admin-server-start
mvn clean install -DskipTests

