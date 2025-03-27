import json
from typing import Dict, Optional
import re
import os
from google import genai
from google.genai import types

from app.core.config import settings
from app.core.logging import logger
from app.utils.llm_parsers import extract_json_from_model_response

class AIService:
    """Base service for AI model interactions using Gemini API"""
    
    def __init__(self):
        self.model = settings.GEMINI_MODEL
        # Get API key from environment or settings
        api_key = settings.GEMINI_API_KEY
        if not api_key:
            raise ValueError("GEMINI_API_KEY not set in environment or configuration")
        # Initialize Google Gemini client
        self.client = genai.Client(api_key=api_key)
        
    async def get_analysis(self, system_prompt: str, user_prompt: str, temperature: float = 0.3) -> Dict:
        """
        Get analysis from Gemini model
        
        Args:
            system_prompt: Instructions for the AI model
            user_prompt: The specific data and questions to analyze
            temperature: Sampling temperature (0-1)
            
        Returns:
            Dict: Parsed JSON response from the model
        """
        try:
            # Prepare the prompt with system instruction and user query
            combined_prompt = f"{system_prompt}\n\n{user_prompt}"
            
            # Create configuration
            config = types.GenerateContentConfig(
                temperature=temperature,
                top_p=0.95,
                top_k=40,
                max_output_tokens=1024,
                safety_settings=[
                    types.SafetySetting(
                        category="HARM_CATEGORY_HARASSMENT",
                        threshold="BLOCK_NONE"
                    ),
                    types.SafetySetting(
                        category="HARM_CATEGORY_HATE_SPEECH",
                        threshold="BLOCK_NONE"
                    ),
                    types.SafetySetting(
                        category="HARM_CATEGORY_SEXUALLY_EXPLICIT",
                        threshold="BLOCK_NONE"
                    ),
                    types.SafetySetting(
                        category="HARM_CATEGORY_DANGEROUS_CONTENT",
                        threshold="BLOCK_NONE"
                    )
                ]
            )
            
            # Make the request to the model
            response = self.client.models.generate_content(
                model=self.model,
                contents=combined_prompt,
                config=config
            )
            
            # Extract the text content
            content = response.text
            
            # Parse the JSON response
            return extract_json_from_model_response(content)
            
        except Exception as e:
            logger.error(f"Error in AI analysis with Gemini: {str(e)}")
            raise