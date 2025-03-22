import logging
from app.core.config import settings

def setup_logging():
    """Configure logging for the application"""
    log_level = getattr(logging, settings.LOG_LEVEL)
    
    # Configure root logger
    logging.basicConfig(
        level=log_level,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )
    
    # Create logger instance
    logger = logging.getLogger("ai_microservice")
    logger.setLevel(log_level)
    
    return logger

# Create application logger
logger = setup_logging()