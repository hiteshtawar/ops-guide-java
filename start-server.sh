#!/bin/bash

# OpsGuide Java Server Startup Script

echo "🚀 Starting OpsGuide Java Server..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "📋 Java version: $JAVA_VERSION"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi

# Set default profile
PROFILE=${1:-dev}

echo "🔧 Starting with profile: $PROFILE"

# Build the application
echo "📦 Building application..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the errors above."
    exit 1
fi

# Start the application
echo "🚀 Starting OpsGuide Java Server..."
echo "📍 Health check: http://localhost:8093/v1/health"
echo "📍 API info: http://localhost:8093/"
echo "📍 Submit request: POST http://localhost:8093/v1/request"
echo ""
echo "🔧 Architecture:"
echo "   1. HTTP Parsing & Validation"
echo "   2. Pattern-based Classification"
echo "   3. Entity Extraction"
echo "   4. Structured Response (Core) OR RAG Pipeline (Premium)"
echo ""
echo "✅ Supported Tasks:"
echo "   • CANCEL_CASE: 'cancel case CASE-2024-001'"
echo "   • UPDATE_CASE_STATUS: 'change case status to completed'"
echo ""
echo "🌐 Server starting..."

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=$PROFILE
