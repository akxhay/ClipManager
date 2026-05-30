# Project Plan

ClipManager: An Android app that monitors the clipboard. It features a foreground notification displaying the last copied text and an option to save copied content to a .txt file. Built with Jetpack Compose and Material Design 3.

## Project Brief

# Project Brief: ClipManager

ClipManager is a utility-focused Android application designed to streamline clipboard management. It provides users with immediate visibility into their copied content and a simple way to persist text snippets for later use, all within a modern Material Design 3 interface.

## Features

- **Clipboard Monitoring Service**: A background service that monitors system clipboard changes in real-time.
- **Dynamic Foreground Notification**: A persistent notification that displays the last copied text, ensuring users always know what's in their buffer.
- **Export to Text File**: A quick-action feature to save the currently copied content directly to a `.txt` file on the device's storage.
- **Material 3 Dashboard**: A vibrant, edge-to-edge UI that displays the current clipboard status and provides a toggle for the monitoring service.

## High-Level Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Asynchronous Programming**: Kotlin Coroutines & Flow
- **Background Processing**: Android Foreground Services (for clipboard monitoring and persistent notification)
- **Code Generation**: KSP (Kotlin Symbol Processing)
- **Core Libraries**: 
    - AndroidX Lifecycle (ViewModel, Compose integration)
    - AndroidX Core KTX
    - File I/O for `.txt` persistence

## Implementation Steps

### Task_1_Core_Service: Implement the Foreground Service to monitor clipboard changes and update a persistent notification. Handle necessary permissions like POST_NOTIFICATIONS and FOREGROUND_SERVICE (with appropriate type for clipboard if applicable).
- **Status:** COMPLETED
- **Updates:** Implemented `ClipboardMonitorService` which utilizes `ClipboardManager.OnPrimaryClipChangedListener` to detect clipboard updates in real-time. Created a persistent notification displaying the most recently copied text. Added required permissions and manifest declarations for Android 14 compatibility. Developed a Material 3 dashboard in `MainActivity` using Jetpack Compose with a runtime permission request flow.
- **Acceptance Criteria:**
  - Foreground service correctly declared in Manifest
  - Service starts and monitors clipboard changes when app is in focus/foreground
  - Persistent notification updates with the last copied text
  - Required permissions are handled/requested

### Task_2_UI_Export: Create the Material 3 dashboard using Jetpack Compose. Include a toggle to start/stop the service and a button to export the current clipboard content to a .txt file using Storage Access Framework.
- **Status:** COMPLETED
- **Updates:** Implemented a Material 3 dashboard using Jetpack Compose with a vibrant, edge-to-edge UI. Added a Status Card with a service toggle and a button to export clipboard content to a .txt file using the Storage Access Framework (SAF). Integrated real-time clipboard updates from the `ClipboardMonitorService` using Kotlin Coroutines Flow. Created a professional adaptive app icon. Updated the color scheme to a vibrant, energetic palette with Material 3 dynamic color support. Verified the build and permission handling.
- **Acceptance Criteria:**
  - UI displays the last copied text and service status
  - Service can be toggled from the UI
  - Clipboard content is successfully saved to a .txt file
  - UI follows Material 3 guidelines
- **Duration:** N/A

### Task_3_Visuals: Refine the application's appearance with a vibrant M3 color scheme, full Edge-to-Edge support, and create an adaptive app icon matching the app's function.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Full Edge-to-Edge display implemented
  - Vibrant Material 3 color scheme applied (Light/Dark themes)
  - Adaptive app icon created and functional
  - UI is energetic and follows Android UX guidelines
- **StartTime:** 2026-03-29 16:41:47 IST

### Task_4_Verify: Run the application and verify all features work as expected. Ensure stability and adherence to the project brief. Instruct critic_agent to verify application stability and report critical UI issues.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Application builds and runs without crashes
  - Clipboard monitoring and notification features are functional
  - Export to .txt works correctly
  - UI is consistent with the Material 3 design system
  - All existing tests pass

