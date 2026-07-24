package com.orbitastra.backend.models.undone.transport;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.base.SchoolBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transport_routes")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportRoute extends SchoolBase {

    private String routeNo;

    private String routeName;

    private List<String> stops;

    private String vehicleNo;

    private String driverName;

    private String driverPhone;
}
