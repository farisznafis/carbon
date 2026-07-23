package com.kct.jcredit.core_backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PublicIdGeneratorTest {

    @Test
    @DisplayName("UT-UTIL-101: Generate Site Public ID - Format Check")
    void testGenerateSitePublicId_Format() {
        String publicId = PublicIdGenerator.generateSitePublicId();
        
        assertNotNull(publicId);
        assertEquals(20, publicId.length());
        assertTrue(publicId.startsWith("SITE"));
        assertTrue(publicId.substring(4).matches("^[0-9A-Za-z]+$"));
    }

    @Test
    @DisplayName("UT-UTIL-102: Generate Site Public ID - Uniqueness Check")
    void testGenerateSitePublicId_Uniqueness() {
        int generateCount = 1000;
        Set<String> ids = new HashSet<>();
        
        for (int i = 0; i < generateCount; i++) {
            ids.add(PublicIdGenerator.generateSitePublicId());
        }
        
        assertEquals(generateCount, ids.size(), "Generated IDs should be unique");
    }
}
