# import os
# from dotenv import load_dotenv

# # Define path to .env file in the parent of the parent directory
# BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))  # Parent of parent of ai_microservice
# dotenv_path = os.path.join(BASE_DIR, ".env")  # Path to .env in the parent of the parent directory

# # Check if the .env file exists
# if not os.path.exists(dotenv_path):
#     raise FileNotFoundError(f".env file not found at {dotenv_path}. Please ensure it exists.")

# # Load the .env file
# load_dotenv(dotenv_path)

# # Test if the environment variable is loaded
# api_key = os.getenv("API_KEY")  # Access the API_KEY variable
# if api_key:
#     print(f"API_KEY loaded successfully: {api_key}")
# else:
#     raise EnvironmentError("API_KEY not found in the .env file. Please ensure it is defined.")

import uvicorn
from app.api.routes import app
from app.core.config import settings

if __name__ == "__main__":
    uvicorn.run(
        "app.api.routes:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=True
    )