# Suara Rumah - Safety MVP

A monitoring and security system (Android-based with a Python backend) designed to detect domestic emergency situations through passive audio analysis and physical hardware triggers.

## Features

- **Passive Audio Monitoring**: Captures audio in the background and extracts sound data directly on the device to detect threats.
- **Hardware SOS Trigger**: Allows users to secretly trigger SOS signals by pressing the physical volume buttons, which is crucial when looking at a screen is impossible.
- **Smart Grace Period**: Provides a specific time delay managed by `GracePeriodManager` allowing users to cancel false alarms before any message is sent.
- **Offline Resilience**: Features a `FailedRequestDao` that securely logs instructions when the connection is lost and automatically resends them when the network is available.
- **Visual Analytics**: Provides data visualization of audio frequencies in graphs and a complete log of alerts via the dashboard.
- **Silent Alert System**: Sends automatic notifications to pre-configured emergency contacts through Twilio API integrations.

## Tech Stack

- **Frontend**: Kotlin, Android, Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel) with Repository Pattern
- **AI & Audio Processing**: Scikit-learn (RandomForestClassifier, Logistic Regression), Librosa
- **Backend**: Python, FastAPI, Pydantic
- **Database & Caching**: Firebase Firestore (NoSQL), Room Database (Local), Redis (In-memory)

## Enhanced Detection Features

### On-Device Audio Processing
Based on our strict privacy principles, the audio processing pipeline is highly optimized:
- **Feature Extraction**: Extracts numerical features (MFCC, RMS, ZCR) directly on the device using Kotlin.
- **Zero Raw Audio**: The raw audio waves are immediately discarded after feature extraction and never transmitted.
- **Fallback Mechanism**: Features simple threshold rules on-device as a backup if the backend is unreachable.

### Smart Backend Classification
- **RandomForest Classifier**: Handles robust classification of audio features and calculates confidence scores.
- **In-Memory Window Tracking**: Uses Redis to track the frequency of detected anomalies over a 30-second window to determine escalation.

## Setup

1. **Install Dependencies**
   - Ensure you have Android Studio installed for the frontend and Python 3.x for the backend.
   - Run `pip install -r requirements.txt` in the backend directory.

2. **Configure API Keys**
   - Configure your Twilio, Firebase, and Backend API keys securely via header implementation.

3. **Run the Services**
   - Start the FastAPI server using Uvicorn or your preferred ASGI server.
   - Build and run the Android application on a physical device (required for microphone and volume button interception testing).

## Usage

1. **Setup Contacts**: Open the app and navigate to `SetupContactScreen` to add emergency contacts to the local Room Database.
2. **Background Monitoring**: The `AudioCaptureService` will run silently in the background.
3. **Hardware Trigger**: In an emergency, press the physical volume buttons to bypass the screen and trigger an alert.
4. **Cancel Alert**: If triggered by mistake, open the app within the 60-second grace period and tap the "Safe" button.

## Privacy & Security

- **No Audio Storage**: The primary database (Firebase Firestore) only stores processed reports, prediction labels, and timestamps—never the raw audio.
- **Subtle Notifications**: Anomaly detection triggers a subtle haptic feedback using `VibrationHelper` rather than obvious screen popups, protecting the victim.
- **API Protection**: Backend endpoints are secured using simple device API keys implemented via middleware headers.

## System Requirements

- **Microphone Access**: Required for background audio capture.
- **Physical Hardware**: Android device with physical volume buttons for the hardware SOS trigger.
- **Network**: Internet connection is required for backend AI analysis and Twilio SMS/WhatsApp routing.

---
*Safety is not just about external threats—Suara Rumah redefines early warning systems by protecting the most private spaces.*
