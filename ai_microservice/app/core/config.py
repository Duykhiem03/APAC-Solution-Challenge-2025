
import os
from dotenv import load_dotenv
from pydantic_settings import BaseSettings

# Load environment variables from .env file
load_dotenv()

class Settings(BaseSettings):
    """Application settings configuration"""
    
    # API Configuration
    API_TITLE: str = "Child Safety AI Microservice"
    API_DESCRIPTION: str = "AI analysis service for child safety monitoring"
    API_VERSION: str = "1.0.0"
    
    # OpenAI Configuration
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    OPENAI_MODEL: str = os.getenv("OPENAI_MODEL", "gpt-4")
    
    # Service Configuration
    HOST: str = os.getenv("HOST", "127.0.0.1")
    PORT: int = int(os.getenv("AI_MICROSERVICE_PORT", "8080"))
    
    # Logging
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")
    
    class Config:
        case_sensitive = True

# Create settings instance
settings = Settings()