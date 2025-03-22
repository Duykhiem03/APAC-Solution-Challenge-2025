from typing import Dict, List, Any
from datetime import datetime

from app.core.logging import logger
from app.services.ai_service import AIService
from app.utils.prompts import construct_route_safety_prompt

class RouteSafetyService:
    """Service for evaluating route safety"""
    
    def __init__(self):
        self.ai_service = AIService()
        
    async def analyze_route_safety(
        self,
        route_points: List[Dict],
        crime_data: List[Dict],
        time_of_day: str,
        user_id: str
    ) -> Dict:
        """
        Analyze the safety of a proposed route
        
        Args:
            route_points: List of geographic coordinates forming the route
            crime_data: Crime incident data near the route
            time_of_day: String indicating time of day (morning, afternoon, evening, night)
            user_id: ID of the user requesting the analysis
            
        Returns:
            Dict: Safety analysis results
        """
        try:
            # Create prompt for AI analysis
            prompt = construct_route_safety_prompt(route_points, crime_data, time_of_day)
            
            # Get analysis from AI model
            system_prompt = "You are an AI specialized in route safety analysis for children."
            safety_analysis = await self.ai_service.get_analysis(system_prompt, prompt, temperature=0.3)
            
            # Add metadata
            safety_analysis["analysis_timestamp"] = datetime.utcnow().isoformat()
            safety_analysis["user_id"] = user_id
            
            return safety_analysis
            
        except Exception as e:
            logger.error(f"Error in route safety analysis: {str(e)}")
            # Fallback to simple rules-based analysis
            return self._fallback_route_safety(route_points, crime_data, time_of_day, user_id)
            
    def _fallback_route_safety(
        self,
        route_points: List[Dict],
        crime_data: List[Dict],
        time_of_day: str,
        user_id: str
    ) -> Dict:
        """Simple rule-based safety analysis as fallback"""
        try:
            # Count nearby crime incidents
            crime_count = len(crime_data)
            
            # Basic safety score based on crime count
            safety_score = max(10 - crime_count, 1) if crime_count < 10 else 1
            
            # Time of day factor - night is considered less safe
            night_hours = ["evening", "night", "late"]
            time_concerns = any(word in time_of_day.lower() for word in night_hours)
            
            if time_concerns:
                safety_score = max(safety_score - 2, 1)
            
            return {
                "safety_score": safety_score,
                "risky_segments": [],  # Cannot determine specific segments in fallback
                "time_of_day_concerns": time_concerns,
                "recommendation": "Consider daytime travel" if time_concerns else "Route appears acceptable",
                "safe_alternative_available": False,
                "is_fallback": True,
                "analysis_timestamp": datetime.utcnow().isoformat(),
                "user_id": user_id
            }
        except Exception as e:
            logger.error(f"Error in fallback route safety: {str(e)}")
            return {
                "safety_score": 5,  # Neutral score
                "risky_segments": [],
                "time_of_day_concerns": False,
                "recommendation": "Error in analysis system",
                "safe_alternative_available": False,
                "is_fallback": True,
                "error": str(e),
                "analysis_timestamp": datetime.utcnow().isoformat(),
                "user_id": user_id
            }