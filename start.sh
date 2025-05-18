#!/bin/bash

# Build the Java backend
echo "Building Java backend..."
mvn clean package

# Start the Java backend in the background
echo "Starting Java backend..."
java -jar target/p2p-1.0-SNAPSHOT.jar &
BACKEND_PID=$!

# Wait for the backend to start
echo "Waiting for backend to start..."
sleep 3

# Install frontend dependencies if node_modules doesn't exist
if [ ! -d "ui/node_modules" ]; then
  echo "Installing frontend dependencies..."
  cd ui && npm install && cd ..
fi

# Start the frontend
echo "Starting frontend..."
cd ui && npm run dev

# When the frontend is stopped, also stop the backend
echo "Stopping backend (PID: $BACKEND_PID)..."
kill $BACKEND_PID
