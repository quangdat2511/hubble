# Hubble — Discord-Inspired Communication Platform

> **Class:** 23KTPM2 &nbsp;|&nbsp; **Course:** Software Development for Mobile Devices &nbsp;|&nbsp; **Group:** 02 &nbsp;|&nbsp; **Date:** 27/01/2026

---

## Table of Contents

1. [Overview](#1-overview)
2. [Tech Stack](#2-tech-stack)
3. [Project Structure](#3-project-structure)
4. [Backend Setup & Run](#4-backend-setup--run)
5. [Features](#5-features)
6. [Assessment](#6-assessment)
7. [Team Members](#7-team-members)

---

## 1. Overview

**Hubble** is a mobile application inspired by **Discord** — the widely-used communication platform that supports real-time messaging, voice calls, and community management.

The goal of this project is to build a feature-complete clone of Discord's core functionalities on Android, while integrating **AI Smart Reply** technology to provide intelligent response suggestions and enhance the overall user experience.

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| **Mobile** | Java 21 (Android), XML Layouts |
| **Backend** | Java 21, Spring Boot 3.2 |
| **Database** | PostgreSQL |
| **Cache / OTP** | Redis |
| **Realtime** | WebSocket (STOMP) |
| **Authentication** | JWT (Access Token + Refresh Token) |
| **File Storage** | AWS S3 |
| **Push Notification** | Firebase Cloud Messaging (FCM) |
| **Email** | SMTP Gmail |
| **AI Smart Reply** | OpenAI API |
| **Build Tool** | Maven (Backend), Gradle (Android) |
| **ORM** | Spring Data JPA / Hibernate |
| **Mapper** | MapStruct |

---

## 3. Project Structure

```
hubble/
├── backend/                        # Spring Boot API Server
│   ├── src/main/java/com/hubble/
│   │   ├── HubbleApplication.java
│   │   ├── configuration/          # SecurityConfig, WebSocketConfig, JwtConfig
│   │   ├── controller/             # REST Controllers
│   │   ├── dto/
│   │   │   ├── request/            # Request bodies
│   │   │   └── response/           # Response bodies
│   │   ├── entity/                 # JPA Entities
│   │   ├── enums/                  # PresenceStatus, MessageType, ServerRole...
│   │   ├── exception/              # ErrorCode, AppException, GlobalExceptionHandler, ApiResponse
│   │   ├── mapper/                 # MapStruct interfaces
│   │   ├── repository/             # Spring Data JPA Repositories
│   │   ├── service/                # Business Logic
│   │   └── validator/              # Custom validation annotations
│   └── src/main/resources/
│       └── application.yml         # Application configuration
│
├── mobile/                         # Android App (Java + XML)
│   ├── app/src/main/
│   │   ├── java/com/hubble/        # Activities, Fragments, ViewModels, Adapters...
│   │   └── res/
│   │       ├── layout/             # XML layout files
│   │       ├── drawable/           # Icons, images
│   │       └── values/             # strings.xml, colors.xml, themes.xml
│   └── build.gradle
├── .gitignore
└── readme.md
```

---

## 4. Backend Setup & Run

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+

### Configuration

Create the PostgreSQL database:

```sql
CREATE DATABASE hubble_db;
```

Edit the connection details in `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hubble_db
    username: <your_username>
    password: <your_password>

jwt:
  secret: <your_secret_key_minimum_32_chars>
```

### Run

```bash
cd backend
mvn spring-boot:run
```

API server starts at: `http://localhost:8080`

### Quick API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register a new account |
| `POST` | `/api/auth/login` | Login |
| `POST` | `/api/auth/refresh` | Refresh access token |
| `GET` | `/api/users/me` | Get current user info |
| `PUT` | `/api/users/{id}` | Update user profile |
| `GET` | `/api/users/{id}` | Get user profile by ID |

---

## 5. Features

### 1. Authentication
- Register / Login with Email or Phone (similar to Discord ID)
- OTP verification (One-Time Password)
- Forgot password & account recovery

### 2. App Security
- App lock with PIN code (Passcode Lock)
- Multitasking screen protection (App Switcher Protection)
- Unknown device login alert (Device Alert)

### 3. User Profile
- Update Avatar (Crop / Rotate image)
- Edit Bio, Status, Nickname
- Personal QR Code (User Tag) for quick friend adding

### 4. Session Management
- View list of active logged-in devices
- Remote logout
- Automatic token renewal (Refresh Token)

### 5. Contacts & Friends
- Find friends by Username / Tag
- Manage friend requests (Send / Receive / Block)

### 6. Presence
- Display status: **Online / Idle / DND / Offline**
- Show "Last seen" timestamp
- Custom status message

### 7. DM List
- List of recent conversations
- Unread message count badge
- Pin important conversations

### 8. Text Chat
- Real-time send & receive (WebSocket)
- Standard Emoji / Icon support
- Sticker & Giphy integration (animated images)

### 9. Message Status
- Display: Sending / Sent / Delivered
- "Typing..." indicator
- Read receipts

### 10. Message Interaction
- Reply & Quote messages
- Forward & Copy content
- Unsend (retract) messages
- Edit sent messages
- Emoji Reactions

### 11. Media Sharing
- Send photos / videos from gallery (Multi-select)
- Capture photos / record videos directly (Camera)
- Media player & inline preview

### 12. Channel System
- Text Channels
- Voice Channels
- Channel Categories
- Private Channel permissions

### 13. Voice Messages
- Quick recording (Hold to record)
- Audio waveform visualization
- Proximity sensor (auto-switch earpiece / speaker)

### 14. File Attachments
- Send documents (PDF, Doc, Zip)
- Upload / Download progress bar
- Open files with third-party apps

### 15. Gallery
- Browse all Media / Links shared in a conversation
- Filter by content type (Images / Videos / Files)
- Download to device

### 16. Group / Server Management
- Create group, set name and avatar
- Manage members (Add / Invite / Kick)
- Role system: Owner, Admin, Member
- @Mention members / groups
- Invite via Link or QR Code

### 17. Search
- Search messages locally (on device) & globally (on server)
- Jump to original message location
- Highlight search keywords
- AI-powered search

### 18. Settings
- Theme: Dark Mode / Light Mode
- Push Notification settings
- App language (Vietnamese / English)

### 19. Notification System
- Push Notifications (FCM)
- In-app alerts
- Email notifications (for important events)

### 20. AI Smart Reply
- Context analysis of incoming messages
- 3 quick reply suggestions (Suggestion Chips)

---

## 6. Assessment

### Benefits

- **High practicality:** Engage with the architecture of a real-world complex application (Discord), tackling real-time communication and multimedia processing challenges.
- **Intelligent experience:** AI Smart Reply integration helps users respond quickly, making the app more convenient and modern.
- **Deep technical skills:** Hands-on experience with WebSocket, REST API, Cloud Storage, application security, and hardware interaction (Camera, Microphone, Proximity Sensor).

### Challenges

- **Data Consistency:** Ensuring message consistency between Client–Server and across multiple devices under unstable network conditions.
- **Performance:** Handling latency when calling the AI service to analyze messages, and optimizing the loading of large message lists and media.
- **Complex Business Logic:** Domain-specific features such as role-based permissions (Roles), presence tracking, and conflict resolution when multiple users chat simultaneously.

---

## 7. Team Members

| No. | Full Name | Student ID |
|---|---|---|
| 1 | Hồ Văn Khải | 23127015 |
| 2 | Nguyễn Hoàng Đăng | 23127166 |
| 3 | Ngô Trần Quang Đạt | 23127341 |
| 4 | Nguyễn Gia Huy | 23127378 |
| 5 | Vũ Văn Vũ | 23127543 |

---

**University of Science, VNU-HCM**  
Instructors: MSc. Trương Toán Thịnh, MSc. Hồ Tuấn Thanh  
Semester 3 — Academic Year 2025–2026
