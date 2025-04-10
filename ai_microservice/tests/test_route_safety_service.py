"""
Tests for the route safety service
"""
import pytest
from unittest.mock import patch

from app.services.route_safety import RouteSafetyService
from tests.test_data import SAMPLE_ROUTE_REQUEST, SAMPLE_ROUTE_RESPONSE


@pytest.mark.asyncio
async def test_analyze_route_safety(mock_ai_service):
    """Test successful route safety analysis"""
    # Configure the mock to return our sample response
    mock_ai_service.get_analysis.return_value = SAMPLE_ROUTE_RESPONSE
    
    # Create service instance with the mock AI service
    service = RouteSafetyService()
    service.ai_service = mock_ai_service
    
    # Call the service method
    result = await service.analyze_route_safety(
        SAMPLE_ROUTE_REQUEST["route_points"],
        SAMPLE_ROUTE_REQUEST["crime_data"],
        SAMPLE_ROUTE_REQUEST["time_of_day"],
        SAMPLE_ROUTE_REQUEST["user_id"]
    )
    
    # Verify the result
    assert result["safety_score"] == 7
    assert len(result["risky_segments"]) == 1
    assert result["user_id"] == "test-user-123"
    
    # Verify AI service was called with appropriate parameters
    mock_ai_service.get_analysis.assert_called_once()
    # Check that the system prompt contains the expected content
    system_prompt = mock_ai_service.get_analysis.call_args[0][0]
    assert "route safety analysis" in system_prompt


@pytest.mark.asyncio
async def test_analyze_route_safety_ai_failure():
    """Test fallback mechanism when AI service fails"""
    # Create service with a mock that raises an exception
    service = RouteSafetyService()
    with patch.object(service, 'ai_service') as mock_ai:
        mock_ai.get_analysis.side_effect = Exception("AI service unavailable")
        
        # Call the service method which should trigger the fallback
        result = await service.analyze_route_safety(
            SAMPLE_ROUTE_REQUEST["route_points"],
            SAMPLE_ROUTE_REQUEST["crime_data"],
            SAMPLE_ROUTE_REQUEST["time_of_day"],
            SAMPLE_ROUTE_REQUEST["user_id"]
        )
        
        # Verify fallback behavior
        assert "is_fallback" in result
        assert result["is_fallback"] is True
        assert "safety_score" in result
        assert "user_id" in result
        assert result["user_id"] == "test-user-123"


@pytest.mark.asyncio
async def test_fallback_route_safety():
    """Test the fallback route safety analysis directly"""
    service = RouteSafetyService()
    
    # Call the fallback method directly
    result = service._fallback_route_safety(
        SAMPLE_ROUTE_REQUEST["route_points"],
        SAMPLE_ROUTE_REQUEST["crime_data"],
        SAMPLE_ROUTE_REQUEST["time_of_day"],
        SAMPLE_ROUTE_REQUEST["user_id"]
    )
    
    # Verify basic fallback functionality
    assert result["is_fallback"] is True
    assert "safety_score" in result
    assert "recommendation" in result
    assert result["user_id"] == "test-user-123"