package com.orbitastra.backend.controllers.student;

import java.util.List;
import java.util.Map;

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

import com.orbitastra.backend.models.student.Guardian;
import com.orbitastra.backend.services.student.GuardianService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;

    @PostMapping
    public ResponseEntity<Guardian> createGuardian(@RequestBody Guardian guardian) {
        return new ResponseEntity<>(guardianService.createGuardian(guardian), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Guardian> getGuardianById(@PathVariable String id) {
        return ResponseEntity.ok(guardianService.getGuardianById(id));
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Guardian>> getGuardiansBySchool(@PathVariable String schoolId) {
        return ResponseEntity.ok(guardianService.getGuardiansBySchool(schoolId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Guardian> updateGuardian(@PathVariable String id, @RequestBody Guardian details) {
        return ResponseEntity.ok(guardianService.updateGuardian(id, details));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGuardian(@PathVariable String id) {
        guardianService.deleteGuardian(id);
        return ResponseEntity.ok(Map.of("message", "Guardian deleted successfully."));
    }
}
