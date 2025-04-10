"""
Tests for the route safety endpoint
"""
import pytest
from tests.test_data import SAMPLE_ROUTE_REQUEST


@pytest.mark.asyncio
async def test_evaluate_route_safety(async_client, mock_route_service):
    """Test that the route safety endpoint processes requests correctly"""
    response = await async_client.post("/analyze/route-safety", json=SAMPLE_ROUTE_REQUEST)
    
    # Check response
    assert response.status_code == 200
    data = response.json()
    assert data["safety_score"] == 7
    assert len(data["risky_segments"]) == 1
    assert data["user_id"] == "test-user-123"
    
    # Verify service called correctly
    mock_route_service.analyze_route_safety.assert_called_once_with(
        SAMPLE_ROUTE_REQUEST["route_points"],
        SAMPLE_ROUTE_REQUEST["crime_data"],
        SAMPLE_ROUTE_REQUEST["time_of_day"],
        SAMPLE_ROUTE_REQUEST["user_id"]
    )


@pytest.mark.asyncio
async def test_evaluate_route_safety_error(async_client, mock_route_service):
    """Test error handling in the route safety endpoint"""
    # Configure mock to raise an exception
    mock_route_service.analyze_route_safety.side_effect = Exception("Test error")
    
    response = await async_client.post("/analyze/route-safety", json=SAMPLE_ROUTE_REQUEST)
    
    # Check error response
    assert response.status_code == 500
    data = response.json()
    assert "detail" in data
    assert "Test error" in data["detail"]


@pytest.mark.asyncio
async def test_evaluate_route_safety_validation_error(async_client):
    """Test validation error handling for invalid requests"""
    # Create an invalid request missing required fields
    invalid_request = {
        "route_points": [],
        # Missing crime_data, time_of_day, and user_id
    }
    
    response = await async_client.post("/analyze/route-safety", json=invalid_request)
    
    # Check validation error response
    assert response.status_code == 422
    data = response.json()
    assert "detail" in data