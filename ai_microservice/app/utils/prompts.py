import json
from typing import List, Dict

def construct_movement_analysis_prompt(historical_data: List[Dict], current_data: Dict) -> str:
    """Create a detailed prompt for movement analysis"""
    return f"""
    Analyze the following movement data for safety concerns:
    
    Historical movement data: {json.dumps(historical_data)}
    Current movement data: {json.dumps(current_data)}
    
    Please analyze for:
    1. Abnormal speed (higher than typical patterns)
    2. Sudden stops in unusual locations
    3. Deviation from usual routes
    4. Any patterns that might indicate safety concerns
    
    Return your analysis in the following JSON format:
    {{
        "abnormal_speed": true/false,
        "sudden_stop": true/false,
        "route_deviation": true/false,
        "safety_concerns": true/false,
        "risk_level": 1-10,
        "reasoning": "Your detailed reasoning here",
        "recommended_action": "Suggested next steps if any"
    }}
    """

def construct_route_safety_prompt(route_points: List[Dict], crime_data: List[Dict], time_of_day: str) -> str:
    """Create a detailed prompt for route safety analysis"""
    return f"""
    Analyze the safety of the following route:
    
    Route points: {json.dumps(route_points)}
    Crime data near route: {json.dumps(crime_data)}
    Time of day: {time_of_day}
    
    Please analyze for:
    1. Overall route safety
    2. Risky segments of the route
    3. Time-of-day considerations
    4. Recommended alternatives if necessary
    
    Return your analysis in the following JSON format:
    {{
        "safety_score": 1-10,
        "risky_segments": [
            {{
                "start_index": int,
                "end_index": int,
                "risk_level": 1-10,
                "reasons": ["reason1", "reason2"]
            }}
        ],
        "time_of_day_concerns": true/false,
        "recommendation": "Your recommendation here",
        "safe_alternative_available": true/false
    }}
    """