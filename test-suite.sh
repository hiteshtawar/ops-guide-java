#!/bin/bash

# OpsGuide Java Test Suite

echo "🧪 Running OpsGuide Java Test Suite..."

BASE_URL="http://localhost:8093"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Function to run a test
run_test() {
    local test_name="$1"
    local method="$2"
    local url="$3"
    local data="$4"
    local expected_status="$5"
    
    echo -n "Testing $test_name... "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "%{http_code}" -o /tmp/response.json "$url")
    else
        response=$(curl -s -w "%{http_code}" -o /tmp/response.json -X "$method" -H "Content-Type: application/json" -H "X-User-ID: test-user" -d "$data" "$url")
    fi
    
    if [ "$response" = "$expected_status" ]; then
        echo -e "${GREEN}✓ PASSED${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAILED${NC} (Expected: $expected_status, Got: $response)"
        ((TESTS_FAILED++))
    fi
}

# Wait for server to be ready
echo "⏳ Waiting for server to be ready..."
for i in {1..30}; do
    if curl -s "$BASE_URL/v1/health" > /dev/null 2>&1; then
        echo "✅ Server is ready!"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

echo ""
echo "🏥 Health Checks"
echo "================"

run_test "Health Check" "GET" "$BASE_URL/v1/health" "" "200"
run_test "API Info" "GET" "$BASE_URL/" "" "200"

echo ""
echo "📦 Core Mode Tests"
echo "=================="

# Cancel Case Tests
run_test "Cancel Case - Core Mode" "POST" "$BASE_URL/v1/request" '{"query": "cancel case CASE-2024-001", "environment": "dev"}' "200"
run_test "Cancel Case - Natural Language" "POST" "$BASE_URL/v1/request" '{"query": "I need to cancel the case CASE-2024-002", "environment": "dev"}' "200"
run_test "Cancel Case - Terminate" "POST" "$BASE_URL/v1/request" '{"query": "terminate case CASE-2024-003", "environment": "dev"}' "200"

# Update Case Status Tests
run_test "Update Case Status - Core Mode" "POST" "$BASE_URL/v1/request" '{"query": "change case status to completed for CASE-2024-001", "environment": "dev"}' "200"
run_test "Update Case Status - Update" "POST" "$BASE_URL/v1/request" '{"query": "update CASE-2024-002 status to accessioning", "environment": "dev"}' "200"
run_test "Update Case Status - Set" "POST" "$BASE_URL/v1/request" '{"query": "set case CASE-2024-003 status to grossing", "environment": "dev"}' "200"

echo ""
echo "🤖 RAG Mode Tests"
echo "================="

# RAG Mode Tests
run_test "Cancel Case - RAG Mode" "POST" "$BASE_URL/v1/request?mode=rag" '{"query": "cancel case CASE-2024-001", "environment": "dev"}' "200"
run_test "Update Case Status - RAG Mode" "POST" "$BASE_URL/v1/request?mode=rag" '{"query": "change case status to completed for CASE-2024-002", "environment": "dev"}' "200"

echo ""
echo "🔧 Case Management API Tests"
echo "============================"

# Case Management API Tests
run_test "Get Case Status" "GET" "$BASE_URL/api/v2/cases/2024-001/status" "" "200"
run_test "Update Case Status API" "PATCH" "$BASE_URL/api/v2/cases/2024-001/status" '{"status": "completed", "reason": "test", "notes": "test"}' "200"
run_test "Cancel Case API" "POST" "$BASE_URL/api/v2/cases/2024-001/cancel" '{"reason": "test", "notes": "test", "notify_stakeholders": true}' "200"
run_test "Preview Cancellation" "GET" "$BASE_URL/api/v2/cases/2024-001/cancel/preview" "" "200"
run_test "Get Case Dependencies" "GET" "$BASE_URL/api/v2/cases/2024-001/dependencies" "" "200"

echo ""
echo "⚠️ Edge Case Tests"
echo "=================="

# Edge Case Tests
run_test "Invalid Query" "POST" "$BASE_URL/v1/request" '{"query": "do something random", "environment": "dev"}' "200"
run_test "Missing User ID" "POST" "$BASE_URL/v1/request" '{"query": "cancel case CASE-2024-001", "environment": "dev"}' "200"
run_test "Case Not Found" "GET" "$BASE_URL/api/v2/cases/999/status" "" "404"

echo ""
echo "📊 Test Results"
echo "==============="
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo "Total Tests: $((TESTS_PASSED + TESTS_FAILED))"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}❌ Some tests failed.${NC}"
    exit 1
fi
