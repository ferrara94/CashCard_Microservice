#!/bin/bash

echo "Building the Docker image for Cashcard..."
docker build -t cashcard_docker -f Docker/Dockerfile .

echo "Running the Docker container..."
docker run -p 8081:8081 cashcard_docker