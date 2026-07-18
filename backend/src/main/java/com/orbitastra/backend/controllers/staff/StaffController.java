package com.orbitastra.backend.controllers.staff;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.orbitastra.backend.dto.staff.CreateStaffRequest;
import com.orbitastra.backend.dto.staff.UpdateStaffRequest;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.services.staff.StaffService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<Staff> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        Staff staff = Staff.builder()
                .schoolId(request.getSchoolId())
                .employeeId(request.getEmployeeId())
                .name(request.getName())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .salary(request.getSalary())
                .joiningDate(request.getJoiningDate())
                .role(request.getRole())
                .dob(request.getDob())
                .build();
        Staff created = staffService.createStaff(staff);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Staff>> getAllStaff() {
        List<Staff> staffList = staffService.getAllStaff();
        return ResponseEntity.ok(staffList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Staff> getStaffById(@PathVariable String id) {
        Staff staff = staffService.getStaffById(id);
        return ResponseEntity.ok(staff);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Staff> getStaffByEmployeeId(@PathVariable String employeeId) {
        Staff staff = staffService.getStaffByEmployeeId(employeeId);
        return ResponseEntity.ok(staff);
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<Staff>> getStaffBySchool(@PathVariable String schoolId) {
        List<Staff> staffList = staffService.getStaffBySchool(schoolId);
        return ResponseEntity.ok(staffList);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Staff> updateStaff(@PathVariable String id, @Valid @RequestBody UpdateStaffRequest request) {
        Staff staffDetails = Staff.builder()
                .employeeId(request.getEmployeeId())
                .name(request.getName())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .salary(request.getSalary())
                .joiningDate(request.getJoiningDate())
                .dob(request.getDob())
                .build();
        Staff updated = staffService.updateStaff(id, staffDetails);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable String id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok(Map.of("message", "Staff deleted successfully."));
    }
}
