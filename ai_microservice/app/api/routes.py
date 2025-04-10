from fastapi import FastAPI, HTTPException, Depends
from typing import Dict

from app.core.config import settings
from app.core.logging import logger
from app.api.models import MovementAnalysisRequest, RouteSafetyRequest, HealthResponse
from app.services.movement import MovementAnalysisService
from app.services.route_safety import RouteSafetyService

# Create FastAPI application
app = FastAPI(
    title=settings.API_TITLE,
    description=settings.API_DESCRIPTION,
    version=settings.API_VERSION
)

# Service dependency injection
def get_movement_service():
    return MovementAnalysisService()

def get_route_safety_service():
    return RouteSafetyService()

@app.post("/analyze/movement", response_model=Dict)
async def analyze_movement_pattern(
    request: MovementAnalysisRequest,
    service: MovementAnalysisService = Depends(get_movement_service)
):
    """Analyze movement patterns to detect anomalies"""
    try:
        logger.info(f"Analyzing movement for user: {request.user_id}")
        analysis_result = await service.analyze_movement(
            request.historical_data,
            request.current_data,
            request.user_id
        )
        return analysis_result
        
    except Exception as e:
        logger.error(f"Error in movement analysis: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Analysis error: {str(e)}")

@app.post("/analyze/route-safety", response_model=Dict)
async def evaluate_route_safety(
    request: RouteSafetyRequest,
    service: RouteSafetyService = Depends(get_route_safety_service)
):
    """Evaluate the safety of a proposed route"""
    try:
        logger.info(f"Analyzing route safety for user: {request.user_id}")
        safety_analysis = await service.analyze_route_safety(
            request.route_points,
            request.crime_data,
            request.time_of_day,
            request.user_id
        )
        return safety_analysis
        
    except Exception as e:
        logger.error(f"Error in route safety analysis: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Analysis error: {str(e)}")

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint"""
    return HealthResponse(status="healthy", service="ai-microservice")