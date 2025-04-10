"""
Shared test data for all test modules
"""

# Test request data
SAMPLE_MOVEMENT_REQUEST = {
    "historical_data": [
        {"timestamp": "2025-01-01T12:00:00", "latitude": 40.7128, "longitude": -74.0060, "speed": 5.0},
        {"timestamp": "2025-01-01T12:05:00", "latitude": 40.7130, "longitude": -74.0062, "speed": 4.8},
        {"timestamp": "2025-01-01T12:10:00", "latitude": 40.7133, "longitude": -74.0065, "speed": 5.2}
    ],
    "current_data": {"timestamp": "2025-01-01T12:15:00", "latitude": 40.7140, "longitude": -74.0070, "speed": 8.5},
    "user_id": "test-user-123"
}

SAMPLE_ROUTE_REQUEST = {
    "route_points": [
        {"latitude": 40.7128, "longitude": -74.0060},
        {"latitude": 40.7150, "longitude": -74.0080},
        {"latitude": 40.7170, "longitude": -74.0100}
    ],
    "crime_data": [
        {"type": "theft", "latitude": 40.7145, "longitude": -74.0075, "timestamp": "2025-01-01T10:30:00"},
        {"type": "assault", "latitude": 40.7155, "longitude": -74.0085, "timestamp": "2025-01-01T20:15:00"}
    ],
    "time_of_day": "afternoon",
    "user_id": "test-user-123"
}

# Test response data
SAMPLE_MOVEMENT_RESPONSE = {
    "abnormal_speed": True,
    "sudden_stop": False,
    "route_deviation": False,
    "safety_concerns": True,
    "risk_level": 6,
    "reasoning": "The current speed (8.5) is significantly higher than the historical average (5.0).",
    "recommended_action": "Monitor the situation",
    "analysis_timestamp": "2025-01-01T12:16:00",
    "user_id": "test-user-123"
}

SAMPLE_ROUTE_RESPONSE = {
    "safety_score": 7,
    "risky_segments": [
        {
            "start_index": 1,
            "end_index": 2, 
            "risk_level": 5,
            "reasons": ["Near recent crime incident"]
        }
    ],
    "time_of_day_concerns": False,
    "recommendation": "Route is generally safe during daytime",
    "safe_alternative_available": True,
    "analysis_timestamp": "2025-01-01T12:16:00",
    "user_id": "test-user-123"
}