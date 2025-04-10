"""
Tests for the movement analysis endpoint
"""
import pytest
from tests.test_data import SAMPLE_MOVEMENT_REQUEST


@pytest.mark.asyncio
async def test_analyze_movement(async_client, mock_movement_service):
    """Test that the movement analysis endpoint processes requests correctly"""
    print(f"Received async_client: {async_client}")
    response = await async_client.post("/analyze/movement", json=SAMPLE_MOVEMENT_REQUEST)
    print(f"Response received: {response.status_code}, {response.json()}")
    
    # Check response
    assert response.status_code == 200
    data = response.json()
    assert data["risk_level"] == 6
    assert data["abnormal_speed"] is True
    assert data["user_id"] == "test-user-123"
    
    # Verify service called correctly
    mock_movement_service.analyze_movement.assert_called_once_with(
        SAMPLE_MOVEMENT_REQUEST["historical_data"],
        SAMPLE_MOVEMENT_REQUEST["current_data"],
        SAMPLE_MOVEMENT_REQUEST["user_id"]
    )


@pytest.mark.asyncio
async def test_analyze_movement_error(async_client, mock_movement_service):
    """Test error handling in the movement analysis endpoint"""
    # Configure mock to raise an exception
    mock_movement_service.analyze_movement.side_effect = Exception("Test error")
    
    response = await async_client.post("/analyze/movement", json=SAMPLE_MOVEMENT_REQUEST)
    
    # Check error response
    assert response.status_code == 500
    data = response.json()
    assert "detail" in data
    assert "Test error" in data["detail"]


@pytest.mark.asyncio
async def test_analyze_movement_validation_error(async_client):
    """Test validation error handling for invalid requests"""
    # Create an invalid request missing required fields
    invalid_request = {
        "historical_data": [],
        # Missing current_data and user_id
    }
    
    response = await async_client.post("/analyze/movement", json=invalid_request)
    
    # Check validation error response
    assert response.status_code == 422
    data = response.json()
    assert "detail" in data