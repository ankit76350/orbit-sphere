package com.orbitastra.backend.services.student;

import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.student.Parent;
import com.orbitastra.backend.repositories.student.ParentRepository;
import com.orbitastra.backend.repositories.core.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;
    private final SchoolRepository schoolRepository;

    public Parent createParent(Parent parent) {
        if (parent.getSchoolId() == null || !schoolRepository.existsById(parent.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + parent.getSchoolId());
        }

        if (parent.getEmail() != null && parentRepository.findByEmail(parent.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Parent with email '" + parent.getEmail() + "' already exists.");
        }

        if (parent.getPhone() != null && parentRepository.findByPhone(parent.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Parent with phone number '" + parent.getPhone() + "' already exists.");
        }

        return parentRepository.save(parent);
    }

    public List<Parent> getAllParents() {
        return parentRepository.findAll();
    }

    public Parent getParentById(String id) {
        return parentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found with id: " + id));
    }

    public Parent getParentByEmail(String email) {
        return parentRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found with email: " + email));
    }

    public List<Parent> getParentsBySchool(String schoolId) {
        return parentRepository.findBySchoolId(schoolId);
    }

    public Parent updateParent(String id, Parent parentDetails) {
        Parent parent = getParentById(id);

        if (parentDetails.getSchoolId() != null && !parentDetails.getSchoolId().equals(parent.getSchoolId())) {
            if (!schoolRepository.existsById(parentDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + parentDetails.getSchoolId());
            }
            parent.setSchoolId(parentDetails.getSchoolId());
        }

        if (parentDetails.getEmail() != null && !parentDetails.getEmail().equals(parent.getEmail())) {
            if (parentRepository.findByEmail(parentDetails.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Parent with email '" + parentDetails.getEmail() + "' already exists.");
            }
            parent.setEmail(parentDetails.getEmail());
        }

        if (parentDetails.getPhone() != null && !parentDetails.getPhone().equals(parent.getPhone())) {
            if (parentRepository.findByPhone(parentDetails.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Parent with phone number '" + parentDetails.getPhone() + "' already exists.");
            }
            parent.setPhone(parentDetails.getPhone());
        }

        if (parentDetails.getFatherName() != null) {
            parent.setFatherName(parentDetails.getFatherName());
        }
        if (parentDetails.getMotherName() != null) {
            parent.setMotherName(parentDetails.getMotherName());
        }
        if (parentDetails.getAlternatePhone() != null) {
            parent.setAlternatePhone(parentDetails.getAlternatePhone());
        }
        if (parentDetails.getAddress() != null) {
            parent.setAddress(parentDetails.getAddress());
        }
        if (parentDetails.getStudentIds() != null) {
            parent.setStudentIds(parentDetails.getStudentIds());
        }

        return parentRepository.save(parent);
    }

    public void deleteParent(String id) {
        Parent parent = getParentById(id);
        parentRepository.delete(parent);
    }
}
