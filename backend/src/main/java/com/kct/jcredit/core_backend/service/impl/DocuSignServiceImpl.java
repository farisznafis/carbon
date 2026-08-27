package com.kct.jcredit.core_backend.service.impl;

import com.kct.jcredit.core_backend.config.DocuSignConfig;
import com.kct.jcredit.core_backend.service.DocuSignService;
import com.kct.jcredit.core_backend.dto.response.DocuSignEnvelopeResult;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
@RequiredArgsConstructor
public class DocuSignServiceImpl implements DocuSignService {

    private final DocuSignConfig config;
    private final RestTemplate restTemplate = new RestTemplate();

    private String cachedToken;
    private Instant tokenExpiresAt;

    private static final long JWT_EXPIRY_SECONDS = 3600;

    // ═════════════════════════════════════════════════════════════
    // AUTH — bagian ini SAMA PERSIS dengan kode lama kamu
    // ═════════════════════════════════════════════════════════════

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

    // ⚠️ Pakai versi auto-detect PKCS#1/PKCS#8 yang sudah terbukti jalan
    //     (bukan versi yang langsung throw IllegalStateException)
    private PrivateKey loadPrivateKey(String path) throws Exception {
        String pem = Files.readString(Paths.get(path));

        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            return loadPkcs1PrivateKey(pem);
        } else {
            return loadPkcs8PrivateKey(pem);
        }
    }

    private PrivateKey loadPkcs8PrivateKey(String pem) throws Exception {
        String privateKeyPEM = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(privateKeyPEM);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private PrivateKey loadPkcs1PrivateKey(String pem) throws Exception {
        String privateKeyPEM = pem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] pkcs1Bytes = Base64.getDecoder().decode(privateKeyPEM);
        byte[] pkcs8Bytes = convertPkcs1ToPkcs8(pkcs1Bytes);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private byte[] convertPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        byte[] pkcs8Header = new byte[] {
                0x30, (byte) 0x82, 0x00, 0x00,
                0x02, 0x01, 0x00,
                0x30, 0x0D,
                0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7,
                0x0D, 0x01, 0x01, 0x01,
                0x05, 0x00,
                0x04, (byte) 0x82, 0x00, 0x00
        };

        int totalLength = pkcs8Header.length - 4 + pkcs1Bytes.length;
        int octetLength = pkcs1Bytes.length;

        pkcs8Header[2] = (byte) ((totalLength >> 8) & 0xFF);
        pkcs8Header[3] = (byte) (totalLength & 0xFF);
        pkcs8Header[pkcs8Header.length - 2] = (byte) ((octetLength >> 8) & 0xFF);
        pkcs8Header[pkcs8Header.length - 1] = (byte) (octetLength & 0xFF);

        byte[] result = new byte[pkcs8Header.length + pkcs1Bytes.length];
        System.arraycopy(pkcs8Header, 0, result, 0, pkcs8Header.length);
        System.arraycopy(pkcs1Bytes, 0, result, pkcs8Header.length, pkcs1Bytes.length);

        return result;
    }

    // ═════════════════════════════════════════════════════════════
    // ENVELOPE — INI YANG BELUM PERNAH MASUK ke file lama kamu
    // ═════════════════════════════════════════════════════════════

    @Override
    public DocuSignEnvelopeResult sendEnvelopeForSignature(byte[] pdfBytes, String signerEmail,
                                                            String signerName, String documentName) {
        try {
            String accessToken = getAccessToken();
            String url = config.getBasePath() + "/v2.1/accounts/" + config.getAccountId() + "/envelopes";

            String base64Doc = Base64.getEncoder().encodeToString(pdfBytes);

            Map<String, Object> document = new HashMap<>();
            document.put("documentBase64", base64Doc);
            document.put("name", documentName);
            document.put("fileExtension", "pdf");
            document.put("documentId", "1");

            Map<String, Object> signHereTab = new HashMap<>();
            signHereTab.put("anchorString", "/sig_sankasha/");
            signHereTab.put("anchorUnits", "pixels");
            signHereTab.put("anchorXOffset", "0");
            signHereTab.put("anchorYOffset", "0");

            Map<String, Object> tabs = new HashMap<>();
            tabs.put("signHereTabs", List.of(signHereTab));

            Map<String, Object> signer = new HashMap<>();
            signer.put("email", signerEmail);
            signer.put("name", signerName);
            signer.put("recipientId", "1");
            signer.put("routingOrder", "1");
            signer.put("tabs", tabs);

            Map<String, Object> recipients = new HashMap<>();
            recipients.put("signers", List.of(signer));

            Map<String, Object> envelopeDefinition = new HashMap<>();
            envelopeDefinition.put("emailSubject", "入会届への署名依頼");
            envelopeDefinition.put("documents", List.of(document));
            envelopeDefinition.put("recipients", recipients);
            envelopeDefinition.put("status", "sent");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(envelopeDefinition, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            String envelopeId = (String) response.getBody().get("envelopeId");
            String status = (String) response.getBody().get("status");

            log.info("[DocuSign] Envelope terkirim. envelopeId={}, status={}", envelopeId, status);

            DocuSignEnvelopeResult result = new DocuSignEnvelopeResult();
            result.setEnvelopeId(envelopeId);
            result.setStatus(status);
            return result;

        } catch (Exception e) {
            log.error("[DocuSign] Gagal kirim envelope. error={}", e.getMessage(), e);
            throw new RuntimeException("DocuSign envelope送信に失敗しました", e);
        }
    }

    @Override
    public byte[] downloadSignedDocument(String envelopeId) {
        try {
            String accessToken = getAccessToken();
            String url = config.getBasePath() + "/v2.1/accounts/" + config.getAccountId()
                    + "/envelopes/" + envelopeId + "/documents/combined";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.APPLICATION_PDF));

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, byte[].class);

            log.info("[DocuSign] Dokumen final berhasil diunduh. envelopeId={}", envelopeId);
            return response.getBody();

        } catch (Exception e) {
            log.error("[DocuSign] Gagal download dokumen final. envelopeId={}, error={}",
                    envelopeId, e.getMessage(), e);
            throw new RuntimeException("DocuSign署名済み文書の取得に失敗しました", e);
        }
    }
}