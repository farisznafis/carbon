package com.kct.jcredit.core_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;

@Entity
@Table(name = "participating_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "site")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParticipatingUser extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "first_name_kana", nullable = false, length = 50)
    private String firstNameKana;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "last_name_kana", nullable = false, length = 50)
    private String lastNameKana;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "user_position", nullable = false, length = 50)
    private String userPosition;
    
    @Column(name = "login_attempts", nullable = false)
    private Integer loginAttempts;

    @Column(name = "account_locked_until", nullable = true)
    private LocalDateTime accountLockedUntil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = true)
    private Site site;

    @Column(name = "company_id", nullable = false, length = 12)
    private String companyId;
}
