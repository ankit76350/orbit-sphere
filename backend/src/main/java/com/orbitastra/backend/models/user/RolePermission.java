package com.orbitastra.backend.models.user;

import com.orbitastra.backend.models.user.enums.AppModule;
import com.orbitastra.backend.models.user.enums.AccessLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermission {
    private AppModule module;
    private AccessLevel accessLevel;
}
