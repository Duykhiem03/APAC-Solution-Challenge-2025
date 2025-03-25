import json
from typing import Dict, Optional
import re
from ollama import AsyncClient  # Import AsyncClient from ollama package

from app.core.config import settings
from app.core.logging import logger
from app.utils.llm_parsers import extract_json_from_model_response

class AIService:
    """Base service for AI model interactions using Ollama"""
    
    def __init__(self):
        self.host = settings.OLLAMA_BASE_URL
        self.model = settings.OLLAMA_MODEL
        self.timeout = settings.OLLAMA_REQUEST_TIMEOUT
        # Create an AsyncClient instance
        self.client = AsyncClient(
            host=self.host,
            timeout=self.timeout,
            headers={"x-app-name": "ChildSafe-AI-Microservice"}
        )
        
    async def get_analysis(self, system_prompt: str, user_prompt: str, temperature: float = 0.3) -> Dict:
        """
        Get analysis from Ollama model using the official Ollama client
        
        Args:
            system_prompt: Instructions for the AI model
            user_prompt: The specific data and questions to analyze
            temperature: Sampling temperature (0-1)
            
        Returns:
            Dict: Parsed JSON response from the model
        """
        try:
            # Create the messages format that Ollama expects
            messages = [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ]
            
            # Use the AsyncClient to make the request
            response = await self.client.chat(
                model=self.model,
                messages=messages,
                stream=False,
                options={
                    "num_ctx": 4096,  # Context window size
                    "temperature": temperature,
                    "num_predict": 800  # Similar to max_tokens
                }
            )
            
            # Extract content from the response
            content = response["message"]["content"]

            json_content = extract_json_from_model_response(content)
            # # Attempt to parse the response as JSON
            return json_content
            
        except json.JSONDecodeError as e:
            logger.error(f"Error parsing response as JSON: {str(e)}")
            # Try to extract JSON from the response if it contains other text
            try:
                # Look for JSON-like patterns in the content
                json_match = re.search(r'(\{.*\})', content, re.DOTALL)
                if json_match:
                    return json.loads(json_match.group(1))
                raise
            except:
                # If that fails, return a simplified response
                logger.error(f"Failed to extract JSON from response: {content[:100]}...")
                return {
                    "error": "Failed to parse model response as JSON",
                    "raw_response": content[:500]  # Include part of the raw response for debugging
                }
                
        except Exception as e:
            logger.error(f"Error in AI analysis with Ollama: {str(e)}")
            raise