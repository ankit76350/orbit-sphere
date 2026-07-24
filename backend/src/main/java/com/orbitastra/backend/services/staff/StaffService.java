package com.orbitastra.backend.services.staff;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.orbitastra.backend.exceptions.ResourceNotFoundException;
import com.orbitastra.backend.models.staff.Staff;
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

        if (staff.getEmployeeNo() != null && !staff.getEmployeeNo().isEmpty()) {
            if (staffRepository.findByEmployeeNo(staff.getEmployeeNo()).isPresent()) {
                throw new IllegalArgumentException(
                        "Employee number '" + staff.getEmployeeNo() + "' is already taken.");
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

    public Staff getStaffByEmployeeNo(String employeeNo) {
        return staffRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff not found with employee number: " + employeeNo));
    }

    public List<Staff> getStaffBySchool(String schoolId) {
        return staffRepository.findBySchoolId(schoolId);
    }

    public Staff updateStaff(String id, Staff staffDetails) {
        Staff staff = getStaffById(id);

        if (staffDetails.getSchoolId() != null && !staffDetails.getSchoolId().equals(staff.getSchoolId())) {
            if (!schoolRepository.existsById(staffDetails.getSchoolId())) {
                throw new ResourceNotFoundException("School not found with id: " + staffDetails.getSchoolId());
            }
            staff.setSchoolId(staffDetails.getSchoolId());
        }

        if (staffDetails.getEmployeeNo() != null && !staffDetails.getEmployeeNo().equals(staff.getEmployeeNo())) {
            if (staffRepository.findByEmployeeNo(staffDetails.getEmployeeNo()).isPresent()) {
                throw new IllegalArgumentException(
                        "Employee number '" + staffDetails.getEmployeeNo() + "' is already taken.");
            }
            staff.setEmployeeNo(staffDetails.getEmployeeNo());
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
