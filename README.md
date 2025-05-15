# ChildSafe

## Project Overview
ChildSafe is a comprehensive mobile application developed for APAC Solution Challenge 2025. The application aims to ensure children's safety during their daily commutes by providing secure navigation that prioritizes child-friendly routes.

## Current Implementation

### Key Features

#### 📍 Real-time GPS Tracking
- Precise location monitoring with Google's Fused Location Provider
- Live location sharing with trusted contacts
- Location history visualization for parents
- Battery-efficient background tracking
- GPS signal quality indicator

#### 🚨 SOS Emergency System
- One-tap SOS button for immediate help
- Voice-activated emergency triggers
- Automatic notifications to trusted contacts with location data
- Continuous location updates during emergencies
- Simple cancellation for false alarms

#### 🧭 Safe Navigation
- Intelligent route planning that prioritizes child-friendly paths
- Avoidance of high-crime areas and unsafe zones
- Time-based route recommendations (safer routes during daytime)
- Integration with Google Maps for reliable navigation
- Customizable safety parameters for different age groups

#### 👪 Trusted Contacts Integration
- Easy setup of family members and trusted adults
- Customizable notification preferences per contact
- Emergency contact prioritization
- Real-time messaging with trusted contacts
- Status updates for emergency acknowledgment

#### 🚶‍♀️ Health Monitoring
- Step counting with daily goals
- Weekly progress tracking
- Social leaderboards for friendly competition
- Activity duration monitoring
- Character-based walking visualization

#### 🤖 AI-Powered Safety
- Abnormal movement detection using sensor fusion
- Route safety analysis with Google's Gemini 2.0 Flash model
- Behavioral pattern recognition
- Contextual safety alerts
- Child-friendly navigation instructions

### How It Works

#### Safe Navigation System
1. The app analyzes multiple potential routes between start and destination points
2. Each route is scored based on safety metrics (crime data, traffic patterns, time of day)
3. Routes passing through designated safe zones receive higher scores
4. The app recommends the safest route based on the comprehensive safety score
5. Real-time navigation guides the child along the selected safe route

#### AI-Powered Safety Analysis
Our route safety analysis is powered by Google's Gemini 2.0 Flash model, which:
- Analyzes route characteristics to identify potential safety concerns
- Evaluates crowd density patterns and historical crime data along routes
- Provides natural language explanations of safety considerations for each route
- Adapts safety recommendations based on time of day and current conditions
- Generates child-friendly navigation instructions that are easy to understand

#### Abnormal Movement Detection
The application uses sensor fusion (accelerometer, gyroscope, and GPS) to detect:
- Sudden changes in movement patterns
- Unusual speeds or accelerations
- Potential falls or accidents
- Deviations from expected routes
- Long periods of inactivity in unusual locations

## Technology Stack

### Mobile Application (Android)
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI**: Jetpack Compose
- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Navigation Component
- **Location Services**: Google Maps API, Fused Location Provider

### Backend
- **Server**: Node.js with Express
- **Database**: Firestore
- **Maps Integration**: Google Maps Directions API

### AI Route Analysis
- **Language**: Python
- **Framework**: FastAPI
- **AI Model**: Google Gemini 2.0 Flash
- **Services**: Route safety analysis service
- **Testing**: Pytest for unit and integration tests

## Project Structure
```
app/                          # Android application
├── src/                      # Source code
│   ├── main/                 # Main application code
│   │   ├── java/com/example/childsafe/
│   │   │   ├── data/         # Data layer
│   │   │   │   ├── api/      # API services
│   │   │   │   │   └── navigation/  # Navigation API
│   │   │   │   ├── model/    # Data models
│   │   │   │   └── repository/ # Repositories
│   │   │   ├── domain/       # Domain layer
│   │   │   │   ├── model/    # Domain models
│   │   │   │   ├── repository/ # Repository interfaces
│   │   │   │   └── usecase/  # Use cases
│   │   │   └── ui/           # Presentation layer
│   │   │       ├── components/ # Reusable UI components
│   │   │       ├── navigation/ # Navigation components
│   │   │       ├── screens/  # App screens
│   │   │       └── viewmodel/ # ViewModels
│   │   └── res/              # Resources
│   ├── androidTest/          # Android tests
│   └── test/                 # Unit tests
│
backend/                      # Node.js backend services
├── controllers/              # API controllers
│   └── routeNavigationControllers.js   # Navigation controllers
├── routes/                   # API route definitions
│   └── routeNavigationRoutes.js        # Navigation routes
└── services/                 # Business logic services
    └── googleMapsService.js            # Maps integration
    └── routeSafetyService.js           # Safety analysis
│
ai_microservice/              # Python-based AI service
├── app/                      # FastAPI application
│   ├── services/             # AI service implementations
│   │   ├── route_safety.py   # Route safety analysis using Gemini 2.0
│   │   └── movement.py       # Movement pattern analysis
│   ├── utils/                # Utility functions
│   │   ├── llm_parsers.py    # Gemini output parsing
│   │   └── prompts.py        # Gemini prompt templates
│   ├── api/                  # API endpoints
│   └── core/                 # Core configurations
└── tests/                    # Test suite for AI services
```

## Future Roadmap

### 🔍 Enhanced Geofencing
- Custom-shaped safe and unsafe zones
- Time-based geofence rules (school hours, curfew)
- Advanced alert customization
- Geofence sharing between trusted users
- Location prediction to prevent boundary crossings

### 💬 Advanced Communication
- In-app voice and video calls during emergencies
- Automated voice responses for children who need assistance
- Integration with school emergency systems
- Multi-language support for international families
- Audio recording during emergencies (with privacy controls)

### 🤖 Next-Gen AI Features
- Predictive route safety analysis
- Crowd behavior assessment
- Voice tone analysis for distress detection
- Computer vision for environmental hazards
- Personalized safety recommendations

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Node.js 16+ and npm
- Python 3.9+
- Firebase account
- Google Maps API key

### Installation

#### Android App
1. Clone the repository
2. Open the project in Android Studio
3. Create a `local.properties` file with your Google Maps API key:
   ```properties
   MAPS_API_KEY=your_api_key_here
   ```
4. Build and run the application

#### Backend
1. Navigate to the `backend` directory
2. Run `npm install` to install dependencies
3. Create a `.env` file with required environment variables:
   ```
   GOOGLE_MAPS_API_KEY=your_api_key_here
   PORT=3000
   ```
4. Run `npm start` to start the server

#### AI Microservice
1. Navigate to the `ai_microservice` directory
2. Create a Python virtual environment: `python -m venv venv`
3. Activate the environment and install dependencies: `pip install -r requirements.txt`
4. Run the service: `python main.py`

## Contribution Guidelines
- Follow the Kotlin style guide for Android development
- Use ESLint rules for JavaScript code
- Write unit tests for all new features
- Create feature branches and submit pull requests for review

## License
MIT License

## Acknowledgments
- Google Developer Groups for organizing the APAC Solution Challenge 2025
- The open-source community for the various libraries used

## Team
Truong Thi Nhat Linh - Business Analyst
Nguyen Hoang Thanh Ngan - UI/UX Designer
Pham Duy Khiem - Developer