package com.orbitastra.backend.controllers.core;

import java.util.List;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orbitastra.backend.dto.core.CreateSchoolRequest;
import com.orbitastra.backend.dto.core.UpdateSchoolRequest;
import com.orbitastra.backend.models.core.School;
import com.orbitastra.backend.services.core.SchoolService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @PostMapping
    public ResponseEntity<School> createSchool(@Valid @RequestBody CreateSchoolRequest request) {
        School school = School.builder()
                .schoolName(request.getSchoolName())
                .subdomain(request.getSubdomain())
                .logo(request.getLogo())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .subscriptionTier(request.getSubscriptionTier())
                .maxStudents(request.getMaxStudents())
                .maxUsers(request.getMaxUsers())
                .build();
        School created = schoolService.createSchool(school);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<School>> getAllSchools() {
        List<School> schools = schoolService.getAllSchools();
        return ResponseEntity.ok(schools);
    }

    @GetMapping("/active")
    public ResponseEntity<List<School>> getActiveSchools() {
        List<School> schools = schoolService.getActiveSchools();
        return ResponseEntity.ok(schools);
    }

    @GetMapping("/{id}")
    public ResponseEntity<School> getSchoolById(@PathVariable String id) {
        School school = schoolService.getSchoolById(id);
        return ResponseEntity.ok(school);
    }

    @GetMapping("/subdomain/{subdomain}")
    public ResponseEntity<School> getSchoolBySubdomain(@PathVariable String subdomain) {
        School school = schoolService.getSchoolBySubdomain(subdomain);
        return ResponseEntity.ok(school);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<School> updateSchool(@PathVariable String id, @Valid @RequestBody UpdateSchoolRequest request) {
        School schoolDetails = School.builder()
                .schoolName(request.getSchoolName())
                .subdomain(request.getSubdomain())
                .logo(request.getLogo())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .subscriptionTier(request.getSubscriptionTier())
                .maxStudents(request.getMaxStudents())
                .maxUsers(request.getMaxUsers())
                .active(request.getActive())
                .build();
        School updated = schoolService.updateSchool(id, schoolDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchool(@PathVariable String id) {
        schoolService.deleteSchool(id);
        return ResponseEntity.ok(java.util.Map.of("message", "School deleted successfully."));
    }
}
