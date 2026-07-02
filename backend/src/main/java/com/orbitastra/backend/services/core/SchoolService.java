package com.orbitastra.backend.services.core;

import java.time.LocalDateTime;
import java.util.List;


import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.repositories.core.SchoolRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;

    public School createSchool(School school) {
        if (school.getSubdomain() != null && schoolRepository.findBySubdomain(school.getSubdomain()).isPresent()) {
            throw new IllegalArgumentException("Subdomain '" + school.getSubdomain() + "' is already taken.");
        }
        if (school.getCreatedAt() == null) {
            school.setCreatedAt(LocalDateTime.now());
        }
        return schoolRepository.save(school);
    }

    public List<School> getAllSchools() {
        return schoolRepository.findAll();
    }

    public List<School> getActiveSchools() {
        return schoolRepository.findByActive(true);
    }

    public School getSchoolById(String id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));
    }

    public School getSchoolBySubdomain(String subdomain) {
        return schoolRepository.findBySubdomain(subdomain)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with subdomain: " + subdomain));
    }

    public School updateSchool(String id, School schoolDetails) {
        School school = getSchoolById(id);

        if (schoolDetails.getSubdomain() != null && !schoolDetails.getSubdomain().equals(school.getSubdomain())) {
            if (schoolRepository.findBySubdomain(schoolDetails.getSubdomain()).isPresent()) {
                throw new IllegalArgumentException("Subdomain '" + schoolDetails.getSubdomain() + "' is already taken.");
            }
            school.setSubdomain(schoolDetails.getSubdomain());
        }

        if (schoolDetails.getSchoolName() != null) {
            school.setSchoolName(schoolDetails.getSchoolName());
        }
        if (schoolDetails.getLogo() != null) {
            school.setLogo(schoolDetails.getLogo());
        }
        if (schoolDetails.getAddress() != null) {
            school.setAddress(schoolDetails.getAddress());
        }
        if (schoolDetails.getPhone() != null) {
            school.setPhone(schoolDetails.getPhone());
        }
        if (schoolDetails.getEmail() != null) {
            school.setEmail(schoolDetails.getEmail());
        }
        if (schoolDetails.getSubscriptionTier() != null) {
            school.setSubscriptionTier(schoolDetails.getSubscriptionTier());
        }
        if (schoolDetails.getMaxStudents() != null) {
            school.setMaxStudents(schoolDetails.getMaxStudents());
        }
        if (schoolDetails.getMaxUsers() != null) {
            school.setMaxUsers(schoolDetails.getMaxUsers());
        }
        if (schoolDetails.getActive() != null) {
            school.setActive(schoolDetails.getActive());
        }

        return schoolRepository.save(school);
    }

    public void deleteSchool(String id) {
        School school = getSchoolById(id);
        schoolRepository.delete(school);
    }
}
