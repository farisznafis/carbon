package com.kct.jcredit.core_backend.service;

import java.io.IOException;

import com.kct.jcredit.core_backend.dto.response.DocuSignEnvelopeResult;

public interface DocuSignService {

    String getAccessToken() throws Exception;

    DocuSignEnvelopeResult sendEnvelopeForSignature(byte[] pdfBytes, String signerEmail,
                                                     String signerName, String documentName);

    byte[] downloadSignedDocument(String envelopeId); 
}
