package com.kct.jcredit.core_backend.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kct.jcredit.core_backend.constant.RoleConstants;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "operating_user",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_operating_user_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_operating_user_email", columnNames = "email")
    },
    indexes = {
        @Index(name = "idx_operating_user_company_id", columnList = "company_id"),
        @Index(name = "idx_operating_user_role", columnList = "role")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OperatingUser extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "first_name_kana", nullable = false, length = 50)
    private String firstNameKana;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "last_name_kana", nullable = false, length = 50)
    private String lastNameKana;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;


    @Column(name = "password", nullable = false, length = 255)
    private String password;



    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = RoleConstants.OPERATOR;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "operating_user_permission",
        joinColumns = @JoinColumn(
            name = "operating_user_id",
            foreignKey = @ForeignKey(name = "fk_user_permission_user")
        ),
        inverseJoinColumns = @JoinColumn(
            name = "permission_id",
            foreignKey = @ForeignKey(name = "fk_user_permission_permission")
        ),
        uniqueConstraints = {
            @UniqueConstraint(
                name = "uk_user_permission",
                columnNames = {"operating_user_id", "permission_id"}
            )
        }
    )
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @Column(name = "login_attempts", nullable = false)
    @Builder.Default
    private Integer loginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "email_otp_login_enabled", nullable = false)
    @Builder.Default
    private Boolean emailOtpLoginEnabled = false;

    /**
     * Increment this whenever roles or permissions change.
     * Existing access tokens can then be invalidated.
     */
    @Column(name = "auth_version", nullable = false)
    @Builder.Default
    private Integer authVersion = 0;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "company_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_operating_user_company")
    )
    private OperatorCompany company;

    public void replacePermissions(Set<Permission> newPermissions) {
        this.permissions.clear();

        if (newPermissions != null) {
            this.permissions.addAll(newPermissions);
        }

        this.authVersion++;
    }

}