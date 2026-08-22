# health — what the school must know when a child is unwell

One question shapes this package, and it has to be answered in seconds:

> **A child has collapsed. What do we need to know, and who do we ring?**

Everything here is built around that. This is **not** a medical history, and the
school is not a clinic. It holds the minimum a school needs to keep a child safe
and to prove what it did.

## Why this package exists

`academics/README.md:246` says it directly:

> `MedicalRecord`: moved out of academics; design it in the health module.

That model was removed and given a destination that did not exist. Until now the
school had nowhere to record an allergy, an inhaler, or which medicine the nurse may
give.

It also replaces the live `models/academics/MedicalRecord`, which held five fields —
student, date, diagnosis, medicines, doctor name — and nothing about whether the
parents were told or what happened to the child afterwards. Those are the two things
a school actually gets asked about.

## Relationship overview

```text
Student
  |
  +--> HealthProfile          one per child, for as long as they are here
  |      +--> HealthAlert[]     allergies, conditions — each with WHAT TO DO
  |      +--> routineMedicineConsentDocsId --> GuardianConsent
  |
  +--> ImmunizationRecord[]   vaccinations, not year-scoped
  |
  +--> ClinicVisit[]          one per trip to the nurse
  |      |
  |      +--> StudentOutPass  (gate) when the child was sent home
  |      +--> GuardianInformed[]  who was told, and when
  |
  +--> MedicationAdministration[]   one per dose given, or not given
```

## The collections

| Collection | Purpose |
|---|---|
| `health_profiles` | One per child. What is always true, and what to do about it. |
| `clinic_visits` | One trip to the nurse. |
| `medication_administrations` | One dose given, or recorded as not given. |
| `immunization_records` | One vaccination. |

`HealthAlert` is embedded in the profile and has no collection of its own.

## Standing facts and events are separate

The split you have seen four times now:

| Always true | Happened on a date |
|---|---|
| `HealthProfile` | `ClinicVisit` |

A child with asthma has **one** profile and, over five years, perhaps forty visits.

The profile is **not** academic-year scoped. A peanut allergy does not end in March,
and making somebody re-enter it every April is how it goes missing in the year it
matters. `ClinicVisit` and `MedicationAdministration` *are* year-scoped, because
"how many children came to the clinic this year" is a real question.

## `whatToDo` is the most important field in the package

An alert is not a list of words. `"Nut allergy"` tells a teacher nothing useful at
the moment it counts.

```java
title    = "Peanut allergy"
severity = LIFE_THREATENING
whatToDo = "Use the adrenaline pen in the staff room, then call an ambulance
            and ring the mother on the number in the profile."
```

That is a plan somebody can follow **while frightened and untrained**. That is the
bar this field has to meet.

`severity` decides how loudly it shows. A nut allergy that can kill and a dislike of
onions are both `ALLERGY`, and showing them identically is how the important one gets
missed.

**Severity alone decides where an alert appears.** `HIGH` and `LIFE_THREATENING` are
pushed onto every screen that names the child, so a teacher taking a trip cannot miss
them. Everything below that stays in the health record.

There is deliberately **no separate "show this one" switch.** A switch would allow an
alert marked `LIFE_THREATENING` with visibility turned off — the one combination that
must never exist. One field, one source of truth, and a fatal allergy cannot be
hidden by accident.

A family wanting a serious condition kept quiet is a real situation, and it is not
solved by hiding an alert. An alert exists so staff can **act**; something nobody may
act on belongs in the support and safeguarding module, where access is narrower than
the whole health team.

## Alerts are deliberately *not* encrypted

Clinical notes are encrypted. Alerts are not, and that is a decision rather than an
oversight.

An alert locked behind a decryption call is an alert nobody reads on a school trip
with no signal. The whole value of `whatToDo` is that it is available instantly, to
whoever is standing there.

What protects it is **who may see the child at all** — the `HEALTH` module in
`identity` — not a cipher. Encryption defends against a stolen database; it does
nothing about the wrong staff member reading a screen, and it actively harms the
emergency case.

`encryptedNotes` on the profile and on each visit *are* encrypted, because a nurse's
written notes are the most private text the school stores and nobody needs them in a
hurry.

## Medication is the serious one

Giving a child medicine is **doing something to them**. If it goes wrong the school
will be asked exactly what was given, how much, by whom, at what time, and who said
it could be. Every one of those is a field.

**Nothing may be given without a consent behind it.** Either:

- the profile's `routineMedicineConsentDocsId` points at a standing `ROUTINE_MEDICATION`
  consent covering ordinary things like paracetamol, and the dose leaves
  `guardianConsentDocsId` null; or
- the dose points at its own `guardianConsentDocsId` — a `RECORD_SPECIFIC`
  `MEDICAL_TREATMENT` consent naming that medicine and those dates.

A dose with neither leaves the school with no defence at all. And the consent has to be
`GRANTED` and unexpired **at the moment of the dose**, not merely present: a family that
withdrew last week has said no, whatever the pointer still says.

Which of the two applied is not a stored flag. A null `guardianConsentDocsId` means the
standing consent was used — a boolean saying so beside it could contradict the pointer next
to it, and an earlier `usedStandingConsent` field did exactly that until it was removed.

### A dose not given is still written down

`MedicationStatus` has three ways of not happening — `REFUSED_BY_STUDENT`,
`WITHHELD`, `MISSED`.

A missing row and a skipped dose look identical, and only one of them needs a phone
call home. A child whose lunchtime inhaler was missed must be **visible**, not
absent from the record.

### Two different people

`administeredByStaffDocsId` gave it. `authorisedByStaffDocsId` said it could be
given. Keeping them apart is the point: one is accountable for the decision, the
other for the act.

## Free text where a school will actually fill it in

The reference sketches had coded symptoms, coded diagnoses and coded medications
against medical dictionaries. All of that is gone.

A school nurse writing down that a mother sent in a bottle of cough syrup is never
going to look up its dictionary code. **A field nobody fills in is worse than no
field** — it looks like data and isn't. So `medicineName`, `vaccineName`,
`reportedComplaint` and `treatmentGiven` are text, recorded exactly as they were
given.

## Being sent home ill is not being absent

`ClinicVisit` does not touch attendance. A child sent home at eleven **was present**
in the morning. How a half day is recorded belongs to the attendance models.

Same rule as the gate log: being in the building is not being in class, and being
unwell is not being away.

## Where health joins the rest of the system

- **`gate`** — `ClinicVisit.studentOutPassDocsId`. Being sent home ill and being
  collected early are one event seen from two places. Joining them means "who took my
  child and when" has one answer instead of two half-answers.
- **`student`** — `Guardian` for who was informed. The profile does not copy phone
  numbers; the guardian record holds them, and `GuardianLink.emergencyContact` says
  who to try first.
- **`documents`** — consent forms, care plans and vaccination cards are all
  `DocumentRecord`s.
- **`identity`** — a new `HEALTH` module, held apart from `STUDENTS` on purpose. A
  class teacher needs a child's timetable, not their medical notes.

## Added to the shared enums

- `AppModule.HEALTH` — so health access can be granted separately from student
  access. Without it, anyone who could see a student could see their medical record.
- `NumberSequenceType.CLINIC_VISIT` — for `ClinicVisit.visitNo`, so a visit can be
  quoted to a parent or a doctor.

## Deliberately left out

- **Coded clinical vocabularies.** See above. A school clinic will never populate
  them.
- **Dental and vision screening campaigns.** Schools run these as one-off drives.
  Worth its own model when somebody asks, not a half-built field now.
- **Counselling and mental-health records.** These need stricter access than
  anything here — often only one named counsellor, not the whole health module — and
  they belong with the support/safeguarding module. Putting them here would give the
  nurse access to them by accident.
- **Insurance claims.** `insuranceReference` is written down so a hospital can use
  it. Actually processing a claim is a finance job.
- **Growth charts and percentiles.** `heightCm` and `weightKg` are recorded; drawing
  a chart is a report, not a model.
- **Notifying a parent automatically.** `guardiansInformed` records that somebody
  told them. Nothing here records a message being sent — that is `notification`,
  which is designed last. Do not add a `notifiedAt` field to get around it.

## Rules the services must enforce

**Access**

1. Reading anything in this package requires the `HEALTH` module. Student access is
   not enough.
2. The only exception is a `HealthAlert` at `HIGH` or `LIFE_THREATENING` severity,
   which any staff member who may see the child may read. Severity is the only thing
   that decides this — there is no per-alert visibility switch to get wrong.
3. Free-text notes are encrypted before saving and decrypted only for a staff member
   with the `HEALTH` module.

**Profiles and alerts**

4. One `HealthProfile` per student.
5. An alert with severity `LIFE_THREATENING` must carry a `whatToDo`. An alert that
   says a child could die and does not say what to do is worse than none.
6. A profile nobody has verified for a long time appears on a list to re-ask the
   family about.

**Medication**

7. No dose is recorded as `GIVEN` without a `GRANTED`, unexpired
   [`GuardianConsent`](../compliance/GuardianConsent.java) behind it — either the profile's
   `routineMedicineConsentDocsId`, or the dose's own `guardianConsentDocsId`. The consent's
   status is checked at the moment of the dose, never assumed from the pointer's presence.
8. The medicine is checked against the child's `ALLERGY` alerts before it is
   recorded, and a match is refused rather than warned about.
9. A status other than `GIVEN` must carry a `notGivenReason`.
10. Rows are never edited. A mistake is corrected by adding a row that explains it.
11. `administeredAt` is set only when the status is `GIVEN`.

**Clinic visits**

12. An outcome of `SENT_HOME`, `REFERRED_TO_HOSPITAL` or `EMERGENCY_SERVICES`
    requires at least one entry in `guardiansInformed`. There is no separate flag to
    set — an empty list is the only way of saying nobody knows, so the claim cannot be
    made without naming who was told and when.
13. Only guardians actually reached go in `guardiansInformed`. Failed attempts go in
    `remarks`; a list of people who did not answer is not a record that anybody was
    told.
14. A child sent home is not marked absent by this package. Attendance is decided
    by the attendance models.
15. `arrivedAt` is never after `leftAt`.

**Immunizations**

16. The same vaccine and dose number is never recorded twice for one child.
17. `administeredOn` is not in the future.
18. `verificationStatus` moves to `VERIFIED` only when a staff member has looked at
    the evidence, and that member is recorded.
