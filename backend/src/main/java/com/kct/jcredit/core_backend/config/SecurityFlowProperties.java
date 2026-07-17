package com.kct.jcredit.core_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityFlowProperties {
    private String frontendBaseUrl = "http://localhost:5173";
    private String tokenPepper = "dev-only-change-me";
    private Duration otpTtl = Duration.ofMinutes(5);
    private Duration otpResendCooldown = Duration.ofSeconds(60);
    private int otpMaxAttempts = 5;
    private int otpMaxSends = 3;
    private Duration otpSendWindow = Duration.ofMinutes(15);
    private Duration masterSetupLinkTtl = Duration.ofHours(72);
    private Duration masterSetupSessionTtl = Duration.ofMinutes(30);
    private Duration revisionLinkTtl = Duration.ofDays(7);
    private Duration revisionSessionTtl = Duration.ofHours(2);
    private Duration passwordResetLinkTtl = Duration.ofMinutes(30);
    private Duration passwordResetSessionTtl = Duration.ofMinutes(15);
    private int terminalRetentionDays = 30;
    private int bcryptStrength = 12;
    private int ipMaxRequests = 20;
    private Duration ipRateWindow = Duration.ofMinutes(15);
    private final Features features = new Features();

    @Getter
    @Setter
    public static class Features {
        private boolean signupEmailVerification;
        private boolean masterSetupLink;
        private boolean rejectedSignupRevision;
        private boolean passwordReset;
        private boolean loginEmailOtp;
    }
}
