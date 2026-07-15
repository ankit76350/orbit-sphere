package com.orbitastra.backend.models.undone.user;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.orbitastra.backend.models.undone.user.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "role_permission_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionMapping {
    @Id
    private String id;

    @Indexed(unique = true)
    private Role role;

    private List<RolePermission> permissions;
}
