"""
Integration tests for the API endpoints
These tests require a running service instance
"""
import pytest
from tests.test_data import SAMPLE_MOVEMENT_REQUEST, SAMPLE_ROUTE_REQUEST
from httpx import Timeout
import json


@pytest.mark.integration
async def test_health_check_integration(api_client):
    """Test health check endpoint on a running service"""
    response = await api_client.get("/health")
    
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "ai-microservice"


@pytest.mark.integration
async def test_movement_analysis_integration(api_client):
    """Test movement analysis endpoint on a running service"""
    response = await api_client.post("/analyze/movement", json=SAMPLE_MOVEMENT_REQUEST)
    
    # Since we're testing against a real service, we just check the structure
    assert response.status_code == 200
    data = response.json()
    assert "risk_level" in data
    assert "abnormal_speed" in data
    assert "reasoning" in data
    assert "user_id" in data
    assert data["user_id"] == SAMPLE_MOVEMENT_REQUEST["user_id"]


@pytest.mark.integration
async def test_route_safety_integration(api_client):
    """Test route safety endpoint on a running service"""
    response = await api_client.post("/analyze/route-safety", json=SAMPLE_ROUTE_REQUEST)
    
    # Since we're testing against a real service, we just check the structure
    assert response.status_code == 200
    data = response.json()
    assert "safety_score" in data
    assert "risky_segments" in data
    assert "recommendation" in data
    assert "user_id" in data
    assert data["user_id"] == SAMPLE_ROUTE_REQUEST["user_id"]