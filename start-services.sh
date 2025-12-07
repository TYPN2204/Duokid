#!/bin/bash

# Start both Java and Python services
echo "🚀 Khởi động Duokid services..."

# Kill existing services if running
pkill -f "java.*spring-boot" 2>/dev/null
pkill -f "python.*main.py" 2>/dev/null
sleep 2

# Start Python service
echo "📡 Khởi động Python service (port 5000)..."
cd /workspaces/Duokid/python-service
nohup python main.py > logs/python.log 2>&1 &
PYTHON_PID=$!
echo "Python PID: $PYTHON_PID"

# Start Java backend
echo "☕ Khởi động Java backend (port 8080)..."
cd /workspaces/Duokid/backend-java
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
nohup ./mvnw spring-boot:run > logs/java.log 2>&1 &
JAVA_PID=$!
echo "Java PID: $JAVA_PID"

# Wait for services to start
echo "⏳ Chờ services khởi động..."
sleep 15

# Check if services are running
echo "✅ Kiểm tra services..."
curl -s http://localhost:5000/health > /dev/null && echo "✓ Python service: OK" || echo "✗ Python service: FAILED"
curl -s http://localhost:8080/login > /dev/null && echo "✓ Java backend: OK" || echo "✗ Java backend: FAILED"

echo "🎉 Duokid services ready!"
echo "📱 Open: http://localhost:8080"
