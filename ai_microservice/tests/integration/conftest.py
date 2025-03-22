"""
Configuration for integration tests
"""
import os
import pytest
from httpx import AsyncClient


@pytest.fixture
def api_base_url():
    """Get the base URL for integration tests from environment variable or use default"""
    return os.getenv("API_TEST_URL", "http://localhost:3002")


@pytest.fixture
async def api_client(api_base_url):
    """Create a client for integration testing against a running service"""
    async with AsyncClient(base_url=api_base_url) as client:
        yield client