"""
Tests for the health check endpoint
"""
import pytest


def test_health_check(test_client):
    """Test that the health check endpoint returns the expected response"""
    response = test_client.get("/health")
    
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "ai-microservice"