import os
import sys
import pytest
import pytest_asyncio
from unittest.mock import AsyncMock, patch
from fastapi.testclient import TestClient
from httpx import AsyncClient, ASGITransport

# Ensure the 'ai_microservice' directory is in the PYTHONPATH
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # Parent of 'tests'
sys.path.insert(0, BASE_DIR)

from app.api.routes import app
from tests.test_data import SAMPLE_MOVEMENT_RESPONSE, SAMPLE_ROUTE_RESPONSE


@pytest.fixture
def test_client():
    """Return a TestClient instance for testing synchronous endpoints"""
    return TestClient(app)


@pytest_asyncio.fixture
async def async_client():
    """Return an AsyncClient instance for testing asynchronous endpoints"""
    print("Initializing AsyncClient...")  # Debug
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        print(f"AsyncClient initialized: {client}, Type: {type(client)}")
        yield client
    print("AsyncClient closed.")  # Debug


@pytest.fixture
def mock_movement_service():
    """Create a mock movement analysis service"""
    with patch("app.api.routes.MovementAnalysisService") as mock_service:
        instance = mock_service.return_value
        instance.analyze_movement = AsyncMock(return_value=SAMPLE_MOVEMENT_RESPONSE)
        yield instance


@pytest.fixture
def mock_route_service():
    """Create a mock route safety service"""
    with patch("app.api.routes.RouteSafetyService") as mock_service:
        instance = mock_service.return_value
        instance.analyze_route_safety = AsyncMock(return_value=SAMPLE_ROUTE_RESPONSE)
        yield instance


@pytest.fixture
def mock_ai_service():
    """Create a mock AI service for service layer tests"""
    with patch("app.services.ai_service.AIService") as mock_service_class:
        instance = mock_service_class.return_value
        instance.get_analysis = AsyncMock(return_value={})
        yield instance