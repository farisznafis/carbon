package com.kct.jcredit.core_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kct.jcredit.core_backend.entity.base.BaseEntity;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "participating_user")
@SQLDelete(sql = "UPDATE participating_user SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "site")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParticipatingUser extends BaseEntity {

    @Column(name = "username", nullable = false, length = 50)
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

    @Column(name = "email_otp_login_enabled", nullable = false)
    @Builder.Default
    private Boolean emailOtpLoginEnabled = false;

    @Column(name = "auth_version", nullable = false)
    @Builder.Default
    private Integer authVersion = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = true)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private ParticipatingCompany company;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}
