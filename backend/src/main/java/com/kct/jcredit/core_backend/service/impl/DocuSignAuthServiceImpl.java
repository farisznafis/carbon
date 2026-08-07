package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.config.DocuSignConfig;
import com.kct.jcredit.core_backend.service.DocuSignAuthService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
@Slf4j
@Service
@RequiredArgsConstructor
public class DocuSignAuthServiceImpl implements DocuSignAuthService {

    private final DocuSignConfig config;
    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedToken;
    private Instant tokenExpiresAt;

    private static final long JWT_EXPIRY_SECONDS = 3600;

    @Override
    public String getAccessToken() throws Exception {
        if (cachedToken != null && tokenExpiresAt != null
                && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedToken;
        }

        String jwtAssertion = buildJwtAssertion();
        String accessToken = requestAccessToken(jwtAssertion);

        cachedToken = accessToken;
        tokenExpiresAt = Instant.now().plusSeconds(JWT_EXPIRY_SECONDS);

        log.info("[DocuSign] Access token diperoleh, berlaku sampai {}", tokenExpiresAt);
        return cachedToken;
    }

    // ─────────────────────────────────────────────────────────────
    // Bangun JWT assertion, ditandatangani pakai private key kita
    // ─────────────────────────────────────────────────────────────

    private String buildJwtAssertion() throws Exception {
        PrivateKey privateKey = loadPrivateKey(config.getPrivateKeyPath());

        Instant now = Instant.now();

        return Jwts.builder()
                .setIssuer(config.getClientId())
                .setSubject(config.getUserId())
                .setAudience(config.getAuthServer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(JWT_EXPIRY_SECONDS)))
                .claim("scope", "signature impersonation")
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private PrivateKey loadPrivateKey(String path) throws Exception {
        String pem = Files.readString(Paths.get(path));

        String privateKeyPEM = pem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);

        try {
            // Coba format PKCS#8 dulu (paling umum)
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            // Fallback: format PKCS#1 (kalau file diawali "BEGIN RSA PRIVATE KEY")
            // perlu konversi manual — kalau kena kasus ini, kabari aku
            throw new IllegalStateException(
                    "Private key format tidak dikenali. Pastikan file .pem format PKCS#8.", e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Tukar JWT assertion dengan access token asli dari DocuSign
    // ─────────────────────────────────────────────────────────────

    private String requestAccessToken(String jwtAssertion) {
        String url = "https://" + config.getAuthServer() + "/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        body.add("assertion", jwtAssertion);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getBody() == null || !response.getBody().containsKey("access_token")) {
            throw new IllegalStateException("DocuSignからaccess_tokenを取得できませんでした: " + response.getBody());
        }

        return (String) response.getBody().get("access_token");
    }
}