package com.orbitastra.backend.services.academics;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.academics.TimetableEvent;
import com.orbitastra.backend.repositories.academics.SchoolClassRepository;
import com.orbitastra.backend.repositories.academics.TimetableEventRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimetableEventService {

    private final TimetableEventRepository timetableEventRepository;
    private final SchoolRepository schoolRepository;
    private final SchoolClassRepository schoolClassRepository;

    public TimetableEvent createTimetableEvent(TimetableEvent event) {
        if (event.getSchoolId() == null || !schoolRepository.existsById(event.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + event.getSchoolId());
        }

        if (event.getClassId() != null && !schoolClassRepository.existsById(event.getClassId())) {
            throw new ResourceNotFoundException("Class not found with id: " + event.getClassId());
        }

        return timetableEventRepository.save(event);
    }

    public List<TimetableEvent> getAllTimetableEvents() {
        return timetableEventRepository.findAll();
    }

    public TimetableEvent getTimetableEventById(String id) {
        return timetableEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable event not found with id: " + id));
    }

    public List<TimetableEvent> getTimetableEventsBySchool(String schoolId) {
        return timetableEventRepository.findBySchoolId(schoolId);
    }

    public List<TimetableEvent> getTimetableEventsByClassAndSchool(String className, String schoolId) {
        return timetableEventRepository.findByClassNameAndSchoolId(className, schoolId);
    }

    public List<TimetableEvent> getTimetableEventsByClassIdAndSchool(String classId, String schoolId) {
        return timetableEventRepository.findByClassIdAndSchoolId(classId, schoolId);
    }

    public TimetableEvent updateTimetableEvent(String id, TimetableEvent eventDetails) {
        TimetableEvent event = getTimetableEventById(id);

        if (eventDetails.getSchoolId() != null && !eventDetails.getSchoolId().equals(event.getSchoolId())) {
            if (!schoolRepository.existsById(eventDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + eventDetails.getSchoolId());
            }
            event.setSchoolId(eventDetails.getSchoolId());
        }

        if (eventDetails.getClassId() != null && !eventDetails.getClassId().equals(event.getClassId())) {
            if (!schoolClassRepository.existsById(eventDetails.getClassId())) {
                throw new ResourceNotFoundException("Class not found with id: " + eventDetails.getClassId());
            }
            event.setClassId(eventDetails.getClassId());
        }

        if (eventDetails.getClassName() != null) {
            event.setClassName(eventDetails.getClassName());
        }
        if (eventDetails.getSubject() != null) {
            event.setSubject(eventDetails.getSubject());
        }
        if (eventDetails.getTeacher() != null) {
            event.setTeacher(eventDetails.getTeacher());
        }
        if (eventDetails.getDay() != null) {
            event.setDay(eventDetails.getDay());
        }
        if (eventDetails.getTime() != null) {
            event.setTime(eventDetails.getTime());
        }
        if (eventDetails.getRoom() != null) {
            event.setRoom(eventDetails.getRoom());
        }

        return timetableEventRepository.save(event);
    }

    public void deleteTimetableEvent(String id) {
        TimetableEvent event = getTimetableEventById(id);
        timetableEventRepository.delete(event);
    }
}
