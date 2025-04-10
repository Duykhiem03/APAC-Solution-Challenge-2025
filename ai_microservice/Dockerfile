FROM python:3.10-slim

# Set working directory in the container
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Copy only requirements file first to leverage Docker cache
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Install Google Generative AI package
RUN pip install --no-cache-dir google-generativeai

# Copy only the necessary application files
COPY app/ ./app/
COPY main.py .

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV HOST=0.0.0.0
ENV AI_MICROSERVICE_PORT=8080

# Expose the application port
EXPOSE 8080

# Run the application with uvicorn server
CMD ["python", "main.py"]