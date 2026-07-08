# Orbit Sphere — Next-Gen School Management System

Orbit Sphere is a modern, comprehensive school management platform designed to bridge the gap between institutions, teachers, parents, and students. It digitizes every facet of school operations—from academics and attendance to finance and transport—delivered through seamless mobile and web interfaces.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Flutter](https://img.shields.io/badge/Flutter-14213d-02569B?logo=flutter)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F-6DB33F?logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-336791?logo=postgresql)

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

### 📱 Mobile (Frontend)
- **Framework**: [Flutter](https://flutter.dev/)
- **State Management**: [Riverpod](https://riverpod.dev/)
- **Architecture**: Clean Architecture with Domain, Data, and Presentation layers
- **Communication**: REST API with JSON Web Tokens (JWT)

### 💻 Backend
- **Language**: [Java 17+](https://www.java.com/)
- **Framework**: [Spring Boot 3](https://spring.io/projects/spring-boot)
- **Security**: Spring Security, JWT, OAuth2
- **ORM**: Hibernate with JPA
- **Database**: PostgreSQL
- **Build Tool**: Gradle
- **Documentation**: Swagger UI & OpenAPI

### 🗄️ Database
- **Engine**: [PostgreSQL](https://www.postgresql.org/)
- **ORM**: [Hibernate](https://hibernate.org/)
- **Design**: Clean Schema with proper normalization and indexing

---

## 📂 Project Structure

The project follows a modular monolithic architecture with a clear separation of concerns.

```
Orbit-Sphere/
├── backend/                    # Spring Boot backend services
│   ├── src/
│   │   ├── main/java/
│   │   │   └── com/orbitastra/backend/
│   │   │       ├── config/             # Security & Configuration
│   │   │       ├── controllers/        # REST API Endpoints
│   │   │       ├── models/             # JPA Entities
│   │   │       ├── repositories/       # Spring Data Repositories
│   │   │       └── services/           # Business Logic
│   │   └── resources/          # Application Properties & SQL
│   └── build.gradle            # Build configuration
│
├── flutter_app/                # Flutter mobile application
│   ├── lib/
│   │   ├── core/               # Utilities & Constants
│   │   ├── data/               # Data Sources & Repositories
│   │   ├── domain/             # Entities & Use Cases
│   │   └── presentation/         # UI Screens & Widgets
│   └── pubspec.yaml            # Dependencies
│
├── api-tester/                 # API testing frontend
├── .gitignore                  # Gitignore files
├── README.md                   # Project overview
└── pom.xml                     # Root POM (if any)
```

---

## 🚀 Getting Started

### Prerequisites
- Java Development Kit (JDK) 17 or higher
- PostgreSQL Database
- Flutter SDK 3.0 or higher

### Backend Setup

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd backend
   ```

2. Configure the database connection in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/orbit_sphere
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. Run the application:
   ```bash
   ./gradlew bootRun
   ```
   The API will be available at `http://localhost:8080`
   Swagger UI: `http://localhost:8080/swagger-ui.html`

### Flutter App Setup

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

## 👥 Team

- [Ankit Kumar Singh](https://github.com/ankit76350)
- [Rohan Shinde](https://github.com/rohanshinde108)
- [Ayush Kumar](https://github.com/)

---

## 📄 Documentation

- [API Documentation](http://localhost:8080/swagger-ui.html)
- [Project Wiki](WIKI_URL_HERE)

---

## 📚 Acknowledgments

- [Spring Boot](https://spring.io/)
- [Flutter](https://flutter.dev/)
- [PostgreSQL](https://www.postgresql.org/)
- [Suprema](https://www.supremainc.com/)
