# Orbit Sphere — Next-Gen School Management System

Orbit Sphere is a modern, comprehensive school management platform designed to bridge the gap between institutions, teachers, parents, and students. It digitizes every facet of school operations—from academics and attendance to finance and transport—delivered through seamless mobile and web interfaces.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)
![Flutter](https://img.shields.io/badge/Flutter-14213d-02569B?logo=flutter)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=spring)
![MongoDB](https://img.shields.io/badge/MongoDB-47A248?logo=mongodb)

## 🚀 Key Features

### 🎓 Academics
- **Multi-School Support** — Secure tenant isolation for multiple educational institutions.
- **Class & Section Management** — Dynamic class structures with optional promotion tracking.
- **Dynamic Timetabling** — AI-powered conflict detection to build error-free schedules.
- **Result Automation** — Grade calculations, percentage computation, and class-wise roll optimization.

### ⏰ Attendance & Monitoring
- **Biometric Integration** — Seamless connection with Suprema BioEntry W2 for automated attendance.
- **Live Tracking** — Real-time monitoring of student and staff presence.
- **Automated Alerts** — Instant notifications for late arrivals, absences, and leave approvals.

### 💰 Finance & Fees
- **Student Wallet** — Parents can maintain a digital wallet balance for quick fee payments.
- **Fee Management** — Flexible fee structures with installment planning and automated reminders.
- **Payment Tracking** — Comprehensive ledger for fees, fines, and financial history.

### 👤 Unified Dashboards
- **Parent Portal** — Track child's performance, attendance, fees, and school announcements.
- **Teacher Dashboard** — Manage classes, assignments, attendance, and student interactions.
- **Admin Dashboard** — Holistic view of school operations, analytics, and management tools.

### 📱 Multi-Platform Experience
- **Flutter Mobile App** — High-performance iOS and Android applications for parents, teachers, and students.
- **Web Admin Portal** — Feature-rich web interface for school administrators and staff.

---

## 🛠️ Technology Stack

### 💻 Web (Frontend)
- **Framework**: [React 19](https://react.dev/) with [Vite](https://vitejs.dev/)
- **Styling**: [Tailwind CSS](https://tailwindcss.com/)
- **Routing**: React Router
- **Communication**: REST API with JSON Web Tokens (JWT)

### 📱 Mobile (Planned)
- **Framework**: [Flutter](https://flutter.dev/)
- **State Management**: [Riverpod](https://riverpod.dev/)
- **Architecture**: Clean Architecture with Domain, Data, and Presentation layers
- **Communication**: REST API with JSON Web Tokens (JWT)

### 🖥️ Backend
- **Language**: [Java 17+](https://www.java.com/)
- **Framework**: [Spring Boot 4](https://spring.io/projects/spring-boot)
- **Security**: Spring Security, JWT, OAuth2
- **Build Tool**: [Maven](https://maven.apache.org/)
- **Documentation**: Swagger UI & OpenAPI

### 🗄️ Database
- **Engine**: [MongoDB](https://www.mongodb.com/)
- **Data Access**: [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb)
- **Design**: Document-oriented schema organized by domain

---

## 📂 Project Structure

The project follows a modular monolithic architecture with a clear separation of concerns.

```
Orbit-Sphere/
├── backend/                    # Spring Boot backend services
│   ├── src/
│   │   ├── main/java/
│   │   │   └── com/orbitastra/backend/
│   │   │       ├── controllers/        # REST API Endpoints (by domain)
│   │   │       ├── dto/                # Request/Response objects
│   │   │       ├── exceptions/         # Exception handling
│   │   │       ├── models/             # MongoDB Documents (by domain)
│   │   │       ├── repositories/       # Spring Data MongoDB Repositories
│   │   │       └── services/           # Business Logic
│   │   └── resources/          # Application config & static assets
│   └── pom.xml                 # Maven build configuration
│
├── frontend/                   # React + Vite web application
│   └── src/                    # Components, pages, and app entry
│
├── flutter_app/                # Flutter mobile application (planned)
│   ├── lib/
│   │   ├── core/               # Utilities & Constants
│   │   ├── data/               # Data Sources & Repositories
│   │   ├── domain/             # Entities & Use Cases
│   │   └── presentation/         # UI Screens & Widgets
│   └── pubspec.yaml            # Dependencies
│
├── api-tester/                 # React app for exercising the API
├── docs/                       # Project documentation
├── .gitignore
└── README.md                   # Project overview
```

---

## 🚀 Getting Started

### Prerequisites
- Java Development Kit (JDK) 17 or higher
- MongoDB (local instance or connection URI)
- Node.js 18+ (for the web frontend)
- Flutter SDK 3.0 or higher (for the mobile app, once added)

### Backend Setup

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd backend
   ```

2. Configure the MongoDB connection in `src/main/resources/application.properties`:
   ```properties
   spring.data.mongodb.uri=mongodb://localhost:27017/orbit_sphere
   ```

3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   The API will be available at `http://localhost:3456`
   Swagger UI: `http://localhost:3456/swagger-ui.html`

### Web Frontend Setup

1. Navigate to the frontend directory:
   ```bash
   cd ../frontend
   ```

2. Install dependencies and start the dev server:
   ```bash
   npm install
   npm run dev
   ```
   The app runs at `http://localhost:1234`.

### Flutter App Setup (Planned)

1. Navigate to the Flutter app directory:
   ```bash
   cd ../flutter_app
   ```

2. Install dependencies:
   ```bash
   flutter pub get
   ```

3. Run the application:
   ```bash
   flutter run
   ```

---

## 🧩 Key Module Details

### Attendance System
- **Biometric Integration**: SDK support for Suprema BioEntry W2 devices.
- **Real-Time Processing**: Webhooks and WebSocket for instant attendance updates.

### Finance Management
- **Fee Engine**: Flexible fee templates with support for installments, waivers, and discounts.
- **Wallet System**: Parents can top-up wallets for seamless fee payments.

### Timetabling
- **Constraint Solving**: Solves complex scheduling conflicts using backtracking algorithms.
- **Resource Allocation**: Optimized teacher and room assignments.

---

## 📝 License

This project is licensed under the [MIT License](LICENSE) - see the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📞 Support

For support, questions, or feature requests, please open an issue on the GitHub repository.

---

## 📄 Documentation

- [API Documentation](http://localhost:3456/swagger-ui.html)
- [Project Wiki](WIKI_URL_HERE)

---

## 📚 Acknowledgments

- [Spring Boot](https://spring.io/)
- [Flutter](https://flutter.dev/)
- [PostgreSQL](https://www.postgresql.org/)
- [Suprema](https://www.supremainc.com/)
