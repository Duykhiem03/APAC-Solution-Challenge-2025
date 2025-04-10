"""
Tests for the movement analysis service
"""
import pytest
from unittest.mock import patch

from app.services.movement import MovementAnalysisService
from tests.test_data import SAMPLE_MOVEMENT_REQUEST, SAMPLE_MOVEMENT_RESPONSE


@pytest.mark.asyncio
async def test_analyze_movement(mock_ai_service):
    """Test successful movement analysis"""
    # Configure the mock to return our sample response
    mock_ai_service.get_analysis.return_value = SAMPLE_MOVEMENT_RESPONSE
    
    # Create service instance with the mock AI service
    service = MovementAnalysisService()
    service.ai_service = mock_ai_service
    
    # Call the service method
    result = await service.analyze_movement(
        SAMPLE_MOVEMENT_REQUEST["historical_data"],
        SAMPLE_MOVEMENT_REQUEST["current_data"],
        SAMPLE_MOVEMENT_REQUEST["user_id"]
    )
    
    # Verify the result
    assert result["risk_level"] == 6
    assert result["abnormal_speed"] is True
    assert result["user_id"] == "test-user-123"
    
    # Verify AI service was called with appropriate parameters
    mock_ai_service.get_analysis.assert_called_once()
    # Check that the system prompt contains the expected content
    system_prompt = mock_ai_service.get_analysis.call_args[0][0]
    assert "analyzing GPS movements" in system_prompt


@pytest.mark.asyncio
async def test_analyze_movement_ai_failure():
    """Test fallback mechanism when AI service fails"""
    # Create service with a mock that raises an exception
    service = MovementAnalysisService()
    with patch.object(service, 'ai_service') as mock_ai:
        mock_ai.get_analysis.side_effect = Exception("AI service unavailable")
        
        # Call the service method which should trigger the fallback
        result = await service.analyze_movement(
            SAMPLE_MOVEMENT_REQUEST["historical_data"],
            SAMPLE_MOVEMENT_REQUEST["current_data"],
            SAMPLE_MOVEMENT_REQUEST["user_id"]
        )
        
        # Verify fallback behavior
        assert "is_fallback" in result
        assert result["is_fallback"] is True
        assert "risk_level" in result
        assert "user_id" in result
        assert result["user_id"] == "test-user-123"


@pytest.mark.asyncio
async def test_fallback_movement_analysis():
    """Test the fallback movement analysis directly"""
    service = MovementAnalysisService()
    
    # Call the fallback method directly
    result = service._fallback_movement_analysis(
        SAMPLE_MOVEMENT_REQUEST["historical_data"],
        SAMPLE_MOVEMENT_REQUEST["current_data"],
        SAMPLE_MOVEMENT_REQUEST["user_id"]
    )
    
    # Verify basic fallback functionality
    assert result["is_fallback"] is True
    assert "abnormal_speed" in result
    assert "risk_level" in result
    assert result["user_id"] == "test-user-123"