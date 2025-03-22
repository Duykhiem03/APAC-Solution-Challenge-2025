from pydantic import BaseModel
from typing import Dict, List, Any, Optional

class MovementAnalysisRequest(BaseModel):
    """Request model for movement pattern analysis"""
    historical_data: List[Dict]
    current_data: Dict
    user_id: str

class RouteSafetyRequest(BaseModel):
    """Request model for route safety evaluation"""
    route_points: List[Dict]
    crime_data: List[Dict]
    time_of_day: str
    user_id: str

class HealthResponse(BaseModel):
    """Response model for health check endpoint"""
    status: str
    service: str