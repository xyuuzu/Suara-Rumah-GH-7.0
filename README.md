# Suara Rumah - Safety MVP

A monitoring and security system (Android-based with a Python backend) designed to detect domestic emergency situations through passive audio analysis, featuring stealthy hardware-based false alarm cancellation.

## Features

- **Passive Audio Monitoring**: Captures audio in the background and extracts sound data directly on the device to detect threats.
- **Hardware Grace Period Cancellation**: The physical volume buttons (`VolumeButtonInterceptor.kt`) are exclusively used to silently cancel the grace period in the event of a false audio detection, without the user needing to turn on or look at the screen.
- **Smart Grace Period**: Provides a specific time delay managed by `GracePeriodManager` allowing users to cancel false alarms before any message is sent.
- **Offline Resilience**: Features a `FailedRequestDao` that securely logs instructions when the connection is lost and automatically resends them when the network is available.
- **Visual Analytics**: Provides data visualization of audio frequencies in graphs and a complete log of alerts via the dashboard.
- **4-Level Escalation Classification**: Backend classifies audio patterns into `normal` / `bantingan` (object thrown/broken) / `teriakan` (screaming) / `darurat_sos` (most extreme signal) — not just a binary threat/no-threat call. `darurat_sos` triggers an alert immediately on first detection; `bantingan`/`teriakan` require 2 detections within a 30-second window before alerting, to reduce false alarms from a single incidental sound.
- **Actionable WhatsApp Alerts**: Alert messages are structured and actionable, not just raw notifications — includes location link, recommended steps (contact/visit the victim, don't post accusations on social media before evidence, gather evidence, contact local police Unit PPA / hotline 110 / SAPA 129 / LBH APIK / Komnas Perempuan for legal support).
- **Audio Evidence Attachment**: Real alerts are automatically sent as two separate WhatsApp messages (text + audio clip) — WhatsApp does not support captions on voice-note-type media, so this was deliberately split after testing confirmed combined messages silently drop the caption.
- **Opt-In Emergency Contacts**: A newly added emergency contact does not immediately start receiving alerts — the system sends a WhatsApp consent request first ("Reply YES/NO"), and only contacts who confirm can receive real alerts, preventing unsolicited emergency messages to someone who never agreed to the role.

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
- **RandomForest Classifier**: Trained on a blend of synthetic data (12,000 samples) and features extracted from real domestic-violence audio recordings (87 samples, oversampled) — 4 output classes with confidence scores per prediction.
- **Window Tracking**: Uses Redis (with automatic in-memory fallback if Redis is unavailable) to track the frequency of detected anomalies over a 30-second window to determine escalation.

## Setup

1. **Install Dependencies**
   - Ensure you have Android Studio installed for the frontend and Python 3.x for the backend.
   - Run `pip install -r requirements.txt` in the backend directory.

2. **Configure Credentials**
   - Set up Twilio (WhatsApp Sandbox), Firebase (Firestore service account), and optionally Redis credentials in the backend's `.env` file — see `BACKEND/README.md` for step-by-step instructions.

3. **Run the Services**
   - Start the FastAPI server using Uvicorn or your preferred ASGI server.
   - Build and run the Android application on a physical device (required for microphone and volume button interception testing).

## Usage

1. **Setup Contacts**: Open the app and navigate to `SetupContactScreen` to add emergency contacts to the local Room Database.
2. **Background Monitoring**: The `AudioCaptureService` will run silently in the background.
3. **Hardware Cancellation**: If the system falsely detects an anomaly, press the physical volume buttons within the grace period to secretly cancel the alert without opening the phone.
4. **In-App Cancellation**: Alternatively, open the app within the grace period and tap the "Safe" button.

## Privacy & Security

- **No Audio Storage**: The primary database (Firebase Firestore) only stores processed reports, prediction labels, and timestamps—never raw audio from the device.
- **Subtle Notifications**: Anomaly detection triggers a subtle haptic feedback using `VibrationHelper` rather than obvious screen popups, protecting the victim.
- **Demo Audio, Not Live Recordings**: The audio clip attached to WhatsApp alerts is a curated sample from the training dataset, not a live recording of the detected incident — the device never transmits raw audio to the backend at all.

### Honest Limitations (Known, Not Hidden)

- **API authentication was removed** (a simple device-ID + API-key scheme was implemented, then deliberately disabled to unblock frontend development speed) — backend endpoints are currently open. This needs to be re-enabled before handling real user data.
- **Real audio training data is still very small** (13 files). The ~89% evaluation accuracy reflects the model learning the current dataset's patterns, not a guarantee of real-world accuracy — more calibration data from physical devices is the top priority next.
- **Audio attachments require a public URL** (ngrok during development, a real domain once deployed) since Twilio cannot fetch media from `localhost`.
- **Twilio is still in Sandbox mode** — recipients must manually "join" the sandbox before they can receive messages; production use requires upgrading to an approved WhatsApp Business number.

## System Requirements

- **Microphone Access**: Required for background audio capture.
- **Physical Hardware**: Android device with physical volume buttons for the hardware cancellation trigger.
- **Network**: Internet connection is required for backend AI analysis and Twilio WhatsApp alert delivery.

---
*Safety is not just about external threats—Suara Rumah redefines early warning systems by protecting the most private spaces.*
