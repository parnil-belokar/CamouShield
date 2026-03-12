# CamouShield

CamouShield is a **desktop security application based on behavioral biometrics**.  
Instead of relying only on passwords, the system continuously analyzes **typing patterns (keystroke dynamics)** and **mouse movement behavior** to verify that the current user is the legitimate owner of the system.

The goal is to provide **continuous authentication** — detecting unauthorized users even after login.

---

## Overview

Traditional authentication methods verify a user only at login. Once logged in, the system assumes the same user continues to operate the machine.

CamouShield improves security by monitoring **behavioral interaction patterns** such as:

- Typing rhythm and keystroke timing
- Mouse movement patterns
- Interaction speed and activity patterns

If the detected behavior deviates significantly from the trained profile, the system can trigger **security actions such as alerts, session monitoring, or re-authentication.**

---

## Key Features

- Continuous user authentication using **behavioral biometrics**
- Real-time **keystroke dynamics monitoring**
- **Mouse movement tracking** for user behavior analysis
- **JavaFX-based GUI dashboard**
- **Session monitoring and security logging**
- Integration with **Firebase Firestore** for storing activity logs and user data
- Detection of **suspicious or anomalous interaction patterns**

---

## Tech Stack

**Programming Language**

- Java

**Frontend**

- JavaFX (Desktop GUI)

**Backend / Cloud**

- Firebase
- Firestore Database

**Concepts Used**

- Behavioral Biometrics
- Continuous Authentication
- User Behavior Monitoring
- Event Listeners (Keyboard & Mouse)

---

## Project Architecture

```
User Interaction
      │
      ▼
Keyboard Listener ──► Keystroke Data
Mouse Listener ─────► Mouse Movement Data
      │
      ▼
Behavior Monitoring Engine
      │
      ▼
Anomaly Detection / Session Validation
      │
      ▼
Security Actions & Logging
      │
      ▼
Firebase Firestore (Cloud Storage)
```

---

## How It Works

1. The system captures **user input events** such as keystrokes and mouse movements.
2. These interactions are analyzed to extract **behavioral patterns**.
3. The system compares current behavior with expected patterns.
4. If unusual activity is detected, the system can trigger **security monitoring or re-authentication mechanisms**.
5. All activity is logged and stored in **Firebase Firestore** for analysis.

---

## Installation

### 1 Clone the repository

### 2 Navigate to the project directory

### 3 Open the project in your IDE

Recommended IDEs:

- IntelliJ IDEA
- Eclipse
- NetBeans

### 4 Run the application

Run the main Java application file from your IDE.

---
## Project Structure

```
CamouShield
│
├── src
│   ├── listeners
│   │   ├── KeyboardListener.java
│   │   └── MouseListener.java
│   │
│   ├── security
│   │   ├── ProtectionManager.java
│   │   └── SessionManager.java
│   │
│   ├── database
│   │   └── FirebaseManager.java
│   │
│   ├── ui
│   │   └── JavaFX UI Components
│   │
│   └── main
│       └── Application Entry Point
│
├── resources
│   └── Application assets and configuration files
│
├── logs
│   └── Security and activity logs
│
└── README.md
```

## Future Improvements

Possible enhancements for the system include:

- Machine learning models for more accurate behavior classification
- Continuous identity scoring
- Real-time anomaly alerts
- Multi-user profile support
- Advanced visualization dashboard
- Integration with operating system security controls

