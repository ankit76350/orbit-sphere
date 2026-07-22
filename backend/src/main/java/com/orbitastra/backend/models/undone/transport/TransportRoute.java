package com.orbitastra.backend.models.undone.transport;

import com.orbitastra.backend.models.BaseDocument;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "transport_routes")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TransportRoute extends BaseDocument {

    private String routeCode;

    private String routeName;

    private List<String> stops;

    private String vehicleNumber;

    private String driverName;

    private String driverPhone;
}
