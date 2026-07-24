# School ERP data model

This diagram is account-independent. It can be opened in any Mermaid-compatible
Markdown viewer, VS Code Mermaid preview, Mermaid Live, or imported into a
diagram editor as `school-erp-data-model.mmd`.

```mermaid
erDiagram
    direction LR

    school {
        string id PK
        string schoolName
        string subdomain UK
        string subscriptionTier
        int maxStudents
        bool active
    }

    academicYear {
        string id PK
        string schoolId FK
        string name UK "unique within school"
        date startDate
        date endDate
    }

    schoolClass {
        string id PK
        string schoolId FK
        string academicYear FK "references AcademicYear.name"
        string name
        json sections "embedded list"
    }

    staff {
        string id PK
        string schoolId FK
        string employeeNo
        string name
        string department
        string designation
        string role
    }

    inquiry {
        string id PK
        string schoolId FK
        string studentName
        string status
        string counselorDocsId FK
        string admissionDocsId FK
        json guardians "embedded lead snapshot"
        json followUps "embedded timeline"
    }

    admission {
        string id PK
        string schoolId FK
        string admissionNo UK
        string inquiryDocsId FK
        string studentName
        string status
        string studentDocsId FK
        date admissionDate
    }

    student {
        string id PK
        string schoolId FK
        string admissionNo UK
        string admissionDocsId FK
        string name
        date dob
        string status
        string currentAcademicRecordDocsId FK
    }

    guardian {
        string id PK
        string schoolId FK
        string name
        string phone
        string email
        string occupation
    }

    studentAcademicRecord {
        string id PK
        string schoolId FK
        string studentDocId FK
        string academicYear FK "references AcademicYear.name"
        string classDocId FK
        string sectionNo
        string studentNo
        string rollNo
    }

    reviewCycle {
        string id PK
        string schoolId FK
        string academicYear FK "references AcademicYear.name"
        string name
        string status
    }

    studentReview {
        string id PK
        string schoolId FK
        string studentDocsId FK
        string teacherDocsId FK
        string reviewCycleDocsId FK
        float academicScore
        float disciplineScore
    }

    teacherPerformanceReview {
        string id PK
        string schoolId FK
        string teacherDocsId FK
        string reviewerDocsId FK
        string reviewCycleDocsId FK
        float rating
        string comments
    }

    school ||--o{ academicYear : owns
    school ||--o{ schoolClass : contains
    school ||--o{ staff : employs
    school ||--o{ inquiry : receives
    school ||--o{ admission : processes
    school ||--o{ student : enrolls
    school ||--o{ guardian : deduplicates
    school ||--o{ studentAcademicRecord : scopes
    school ||--o{ reviewCycle : runs
    school ||--o{ studentReview : scopes
    school ||--o{ teacherPerformanceReview : scopes

    academicYear ||--o{ schoolClass : scopes
    academicYear ||--o{ studentAcademicRecord : scopes
    academicYear ||--o{ reviewCycle : scopes

    staff ||--o{ inquiry : counsels
    inquiry ||--o| admission : becomes
    admission ||--o| student : converts_to

    student }o--o{ guardian : links_via_guardian_links
    student ||--o{ studentAcademicRecord : has_year_records
    schoolClass ||--o{ studentAcademicRecord : places

    reviewCycle ||--o{ studentReview : contains
    staff ||--o{ studentReview : writes
    student ||--o{ studentReview : receives

    reviewCycle ||--o{ teacherPerformanceReview : contains
    staff ||--o{ teacherPerformanceReview : participates_in
```

## Main relationship flow

`School` scopes every domain. `AcademicYear` scopes classes, student academic
records, and review cycles. CRM `Inquiry` records can become an `Admission`,
which can become a `Student`. Students link to shared `Guardian` documents via
embedded `GuardianLink` values and to year-specific `StudentAcademicRecord`
documents. Staff members counsel inquiries and participate in student and
teacher reviews.
