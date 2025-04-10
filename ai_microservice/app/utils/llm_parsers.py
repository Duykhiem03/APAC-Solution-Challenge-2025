import re
import json

# Extracting JSON from Model Response
def extract_json_from_model_response(response_text):
    """Extract only the JSON object from a response containing thinking tags"""
    
    # Remove <think>...</think> section
    without_thinking = re.sub(r'<think>[\s\S]*?</think>', '', response_text).strip()
    
    # Extract JSON from code block
    json_match = re.search(r'```json\s*([\s\S]*?)\s*```', without_thinking)
    if json_match:
        json_str = json_match.group(1).strip()
        return json.loads(json_str)
    
    # Fallback: try to find any JSON-like object
    json_match = re.search(r'(\{[\s\S]*\})', without_thinking)
    if json_match:
        json_str = json_match.group(1).strip()
        return json.loads(json_str)
        
    raise ValueError("No JSON object found in response")