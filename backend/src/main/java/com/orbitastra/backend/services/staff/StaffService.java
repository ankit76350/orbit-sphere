package com.orbitastra.backend.services.staff;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.models.user.enums.Role;
import com.orbitastra.backend.repositories.core.SchoolRepository;
import com.orbitastra.backend.repositories.staff.StaffRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;
    private final SchoolRepository schoolRepository;

    public Staff createStaff(Staff staff) {
        if (staff.getSchoolId() == null) {
            throw new IllegalArgumentException("School ID cannot be null.");
        }
        if (!schoolRepository.existsById(staff.getSchoolId())) {
            throw new ResourceNotFoundException("School not found with id: " + staff.getSchoolId());
        }

        if (staff.getEmployeeId() != null && !staff.getEmployeeId().isEmpty()) {
            if (staffRepository.findByEmployeeId(staff.getEmployeeId()).isPresent()) {
                throw new IllegalArgumentException("Employee ID '" + staff.getEmployeeId() + "' is already taken.");
            }
        }

        staff.setCreatedAt(LocalDateTime.now());
        staff.setUpdatedAt(LocalDateTime.now());
        return staffRepository.save(staff);
    }

    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    public Staff getStaffById(String id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + id));
    }

    public Staff getStaffByEmployeeId(String employeeId) {
        return staffRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with employee ID: " + employeeId));
    }

    public List<Staff> getStaffBySchool(String schoolId) {
        return staffRepository.findBySchoolId(schoolId);
    }

    public List<Staff> getStaffByRole(Role role) {
        return staffRepository.findByRole(role);
    }

    public List<Staff> getStaffBySchoolAndRole(String schoolId, Role role) {
        return staffRepository.findBySchoolIdAndRole(schoolId, role);
    }

    public Staff updateStaff(String id, Staff staffDetails) {
        Staff staff = getStaffById(id);

        if (staffDetails.getSchoolId() != null && !staffDetails.getSchoolId().equals(staff.getSchoolId())) {
            if (!schoolRepository.existsById(staffDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + staffDetails.getSchoolId());
            }
            staff.setSchoolId(staffDetails.getSchoolId());
        }

        if (staffDetails.getEmployeeId() != null && !staffDetails.getEmployeeId().equals(staff.getEmployeeId())) {
            if (staffRepository.findByEmployeeId(staffDetails.getEmployeeId()).isPresent()) {
                throw new IllegalArgumentException("Employee ID '" + staffDetails.getEmployeeId() + "' is already taken.");
            }
            staff.setEmployeeId(staffDetails.getEmployeeId());
        }

        if (staffDetails.getName() != null) {
            staff.setName(staffDetails.getName());
        }
        if (staffDetails.getDepartment() != null) {
            staff.setDepartment(staffDetails.getDepartment());
        }
        if (staffDetails.getDesignation() != null) {
            staff.setDesignation(staffDetails.getDesignation());
        }
        if (staffDetails.getSalary() != null) {
            staff.setSalary(staffDetails.getSalary());
        }
        if (staffDetails.getJoiningDate() != null) {
            staff.setJoiningDate(staffDetails.getJoiningDate());
        }
        if (staffDetails.getRole() != null) {
            staff.setRole(staffDetails.getRole());
        }
        if (staffDetails.getDob() != null) {
            staff.setDob(staffDetails.getDob());
        }

        staff.setUpdatedAt(LocalDateTime.now());
        return staffRepository.save(staff);
    }

    public void deleteStaff(String id) {
        Staff staff = getStaffById(id);
        staffRepository.delete(staff);
    }
}
