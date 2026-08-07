package com.kct.jcredit.core_backend.entity;


import jakarta.persistence.*;
import lombok.*;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import com.kct.jcredit.core_backend.constant.PermissionCode;

@Entity
@Table(
    name = "permission",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_permission_code", columnNames = "code")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private PermissionCode code;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 50)
    private String category;
}
