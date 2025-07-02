#!/bin/bash
# Build script for Render deployment

echo "Starting Maven build..."
mvn clean package -DskipTests

echo "Build completed successfully!"
echo "JAR file created: target/myApp-0.0.1-SNAPSHOT.jar"

# Verify the JAR file exists
if [ -f "target/myApp-0.0.1-SNAPSHOT.jar" ]; then
    echo "? JAR file verified"
    ls -la target/myApp-0.0.1-SNAPSHOT.jar
else
    echo "? JAR file not found!"
    exit 1
fi
