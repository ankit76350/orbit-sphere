package com.orbitastra.backend.repositories.staff;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.orbitastra.backend.models.staff.Staff;
import com.orbitastra.backend.models.user.enums.Role;

@Repository
public interface StaffRepository extends MongoRepository<Staff, String> {
    List<Staff> findBySchoolId(String schoolId);
    Optional<Staff> findByEmployeeId(String employeeId);
    List<Staff> findByRole(Role role);
    List<Staff> findBySchoolIdAndRole(String schoolId, Role role);
}
