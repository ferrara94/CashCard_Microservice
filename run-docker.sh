#!/bin/bash

echo "Building the Docker image for Cashcard..."
docker build -t cashcard ./Docker

echo "Running the Docker container..."
docker run -p 8080:8080 cashcard