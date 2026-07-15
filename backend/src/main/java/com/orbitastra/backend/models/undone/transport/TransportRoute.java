package com.orbitastra.backend.models.undone.transport;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transport_routes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportRoute {
    @org.springframework.data.annotation.CreatedDate
    private java.time.LocalDateTime createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    private java.time.LocalDateTime updatedAt;


    @Id
    private String id;

    private String schoolId;

    private String routeCode;

    private String routeName;

    private List<String> stops;

    private String vehicleNumber;

    private String driverName;

    private String driverPhone;
}
