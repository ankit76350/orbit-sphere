package com.orbitastra.backend.controllers.student;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.models.student.Parent;
import com.orbitastra.backend.services.student.ParentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PostMapping
    public ResponseEntity<Parent> createParent(@RequestBody Parent parent) {
        Parent created = parentService.createParent(parent);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Parent>> getAllParents() {
        List<Parent> parents = parentService.getAllParents();
        return ResponseEntity.ok(parents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Parent> getParentById(@PathVariable String id) {
        Parent parent = parentService.getParentById(id);
        return ResponseEntity.ok(parent);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Parent> getParentByEmail(@PathVariable String email) {
        Parent parent = parentService.getParentByEmail(email);
        return ResponseEntity.ok(parent);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Parent>> getParentsBySchool(@PathVariable String schoolId) {
        List<Parent> parents = parentService.getParentsBySchool(schoolId);
        return ResponseEntity.ok(parents);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Parent> updateParent(@PathVariable String id, @RequestBody Parent parentDetails) {
        Parent updated = parentService.updateParent(id, parentDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteParent(@PathVariable String id) {
        parentService.deleteParent(id);
        return ResponseEntity.ok(Map.of("message", "Parent deleted successfully."));
    }
}
