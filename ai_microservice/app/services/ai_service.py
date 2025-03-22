import json
from openai import OpenAI
from datetime import datetime
from typing import Dict, List, Any

from app.core.config import settings
from app.core.logging import logger


class AIService:
    """Base service for AI model interactions"""
    
    def __init__(self):
        self.api_key = settings.OPENAI_API_KEY
        self.model = settings.OPENAI_MODEL
        self.client = OpenAI(api_key=self.api_key)
        
    async def get_analysis(self, system_prompt: str, user_prompt: str, temperature: float = 0.3) -> Dict:
        """
        Get analysis from OpenAI model
        
        Args:
            system_prompt: Instructions for the AI model
            user_prompt: The specific data and questions to analyze
            temperature: Sampling temperature (0-1)
            
        Returns:
            Dict: Parsed JSON response from the model
        """
        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                temperature=temperature,
                max_tokens=800
            )
            
            # Parse the response content as JSON
            content = response.choices[0].message.content
            return json.loads(content)
            
        except Exception as e:
            logger.error(f"Error in AI analysis: {str(e)}")
            raise