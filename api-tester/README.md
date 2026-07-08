# School Edu Sphere — API Tester

A standalone React app for exercising the backend API with all its edge cases.

## Stack
Vite 6 · React 19 · Tailwind v4 · lucide-react

## Run
```bash
cd api-tester
npm install
npm run dev        # http://localhost:3100
```
The dev server proxies `/api/*` to the backend at `http://localhost:5030`
(configured in `vite.config.js`), so start the Spring backend first.

## How it works
- **Top bar** — pick a School once; it auto-loads that school's academic years,
  classes, teachers and students. Selections fill `{{token}}` placeholders used
  across every request, so you never paste an ObjectId.
- **Sidebar** — every endpoint grouped by module, filterable.
- **Endpoint panel** — live URL preview, editable path/query, and (for writes) a
  JSON body editor with one-click **edge-case presets** (green = valid,
  red = a deliberate failure case).
- **Response panel** — colour-coded status, latency, pretty JSON.
- **History** — your last 25 calls.

## Extending
Add endpoints/edge-cases declaratively in `src/data/catalog.js`. Bodies may use
`{{schoolId}}`, `{{academicYear}}`, `{{academicYearId}}`, `{{classId}}`,
`{{section}}`, `{{teacherId}}`, `{{teacherId2}}`, `{{studentId}}`, `{{entityId}}`.
