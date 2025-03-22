from typing import Dict, List, Any
from datetime import datetime

from app.core.logging import logger
from app.services.ai_service import AIService
from app.utils.prompts import construct_movement_analysis_prompt

class MovementAnalysisService:
    """Service for analyzing movement patterns"""
    
    def __init__(self):
        self.ai_service = AIService()
        
    async def analyze_movement(
        self,
        historical_data: List[Dict],
        current_data: Dict,
        user_id: str
    ) -> Dict:
        """
        Analyze movement patterns to detect anomalies
        
        Args:
            historical_data: List of historical location points
            current_data: Current location data point
            user_id: ID of the user being analyzed
            
        Returns:
            Dict: Analysis results with risk assessment
        """
        try:
            # Create prompt for AI analysis
            prompt = construct_movement_analysis_prompt(historical_data, current_data)
            
            # Get analysis from AI model
            system_prompt = "You are an AI specialized in analyzing GPS movements to detect potential safety issues for children."
            analysis_result = await self.ai_service.get_analysis(system_prompt, prompt, temperature=0.2)
            
            # Add metadata
            analysis_result["analysis_timestamp"] = datetime.utcnow().isoformat()
            analysis_result["user_id"] = user_id
            
            return analysis_result
            
        except Exception as e:
            logger.error(f"Error in movement analysis: {str(e)}")
            # Fallback to simple rules-based analysis
            return self._fallback_movement_analysis(historical_data, current_data, user_id)
            
    def _fallback_movement_analysis(
        self,
        historical_data: List[Dict],
        current_data: Dict,
        user_id: str
    ) -> Dict:
        """Simple rule-based analysis as fallback when AI analysis fails"""
        try:
            # Extract current speed
            current_speed = current_data.get("speed", 0)
            
            # Get historical speeds
            historical_speeds = [point.get("speed", 0) for point in historical_data 
                                if point.get("speed") is not None]
            
            # Calculate average and max historical speed
            avg_speed = sum(historical_speeds) / len(historical_speeds) if historical_speeds else 0
            max_speed = max(historical_speeds) if historical_speeds else 0
            
            # Check for abnormal speed
            abnormal_speed = current_speed > max_speed * 1.2 or current_speed > avg_speed * 1.5
            
            # Basic risk assessment
            risk_level = 7 if abnormal_speed else 2
            
            return {
                "abnormal_speed": abnormal_speed,
                "sudden_stop": False,  # Cannot determine in simple fallback
                "route_deviation": False,  # Cannot determine in simple fallback
                "safety_concerns": abnormal_speed,
                "risk_level": risk_level,
                "reasoning": "Fallback analysis: Speed analysis only",
                "recommended_action": "Monitor speed" if abnormal_speed else "No action needed",
                "is_fallback": True,
                "analysis_timestamp": datetime.utcnow().isoformat(),
                "user_id": user_id
            }
        except Exception as e:
            logger.error(f"Error in fallback analysis: {str(e)}")
            return {
                "abnormal_speed": False,
                "sudden_stop": False,
                "route_deviation": False,
                "safety_concerns": False,
                "risk_level": 1,
                "reasoning": "Error in analysis system",
                "recommended_action": "Check system logs",
                "is_fallback": True,
                "error": str(e),
                "analysis_timestamp": datetime.utcnow().isoformat(),
                "user_id": user_id
            }