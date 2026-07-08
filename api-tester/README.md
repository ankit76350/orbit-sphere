# Timetable & Core API Tester Console

This is a standalone ReactJS frontend console designed to test the REST endpoints in your Spring Boot backend (under `com.orbitastra.backend.controllers.core`).

## Supported APIs & Operations

1. **Schools (`/api/schools`)**
   - Create new school instances (POST)
   - Update existing schools by patching properties (PATCH)
   - Fetch active/all schools and query by subdomain or ID (GET)
   - Remove school entries (DELETE)

2. **Academic Years (`/api/academic-years`)**
   - Create and update academic year calendars (POST / PUT)
   - Retrieve by ID, school, date, and name (GET)
   - Manage holiday rosters and configure weekly-off series (POST / DELETE / GET)

3. **Announcements (`/api/announcements`)**
   - Post bulletins for the institutional billboard (POST)
   - Retrieve all announcements, query by school/target, and edit/delete entries (GET / PUT / DELETE)

4. **Notifications (`/api/notifications`)**
   - Dispatch alerts via email, push, SMS, and WhatsApp (POST)
   - Query logs by recipient/school, mark as sent, and prune records (GET / PUT / DELETE)

## Development Configuration

- The Vite dev server runs on port **`3001`**.
- It is pre-configured with a Vite server proxy that forwards `/api/*` requests to your Spring Boot backend on `http://localhost:5030` to avoid CORS issues during testing.

## Getting Started

1. Ensure the backend server is running:
   ```bash
   cd ../backend
   mvn spring-boot:run
   ```
   (Verify backend is reachable on `http://localhost:5030`)

2. Install dependencies (if not already done):
   ```bash
   npm install
   ```

3. Launch the API Tester dev server:
   ```bash
   npm run dev
   ```

4. Open [http://localhost:3001](http://localhost:3001) in your browser.
