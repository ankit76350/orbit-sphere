package com.orbitastra.backend.models.undone.user;

import com.orbitastra.backend.models.undone.user.enums.AccessLevel;
import com.orbitastra.backend.models.undone.user.enums.AppModule;

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
