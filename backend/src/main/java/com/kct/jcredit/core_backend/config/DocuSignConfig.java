package com.kct.jcredit.core_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "docusign")
@Getter @Setter
public class DocuSignConfig {
    private String clientId;
    private String userId;
    private String accountId;
    private String authServer;
    private String basePath;
    private String privateKeyPath;
}
