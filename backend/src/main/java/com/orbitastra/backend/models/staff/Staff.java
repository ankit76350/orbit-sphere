package com.orbitastra.backend.models.staff;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;
import com.orbitastra.backend.models.undone.user.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "staffs")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Staff extends SchoolBase {

    private String employeeNo;

    private String name;

    private String department;

    private String designation;

    private BigDecimal salary;

    private LocalDate joiningDate;

    private Role role;

    private LocalDate dob;
}



Part 3
TransportRoute.java
RouteStop.java
GeoLocation.java



Part 4
RouteAssignment.java
AssignmentStatus.java



Part 5
TransportAllocation.java
AllocationStatus.java



Part 6
VehicleLocation.java
TripStatus.java


Part 7
VehicleLocationHistory.java



Part 8
TransportAttendance.java
AttendanceType.java
AttendanceStatus.java
