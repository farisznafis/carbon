package com.kct.jcredit.core_backend.service;

import com.kct.jcredit.core_backend.dto.request.EnR002EquipmentRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002FinalAgreementRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002SustainabilityCommitmentRequest;
import com.kct.jcredit.core_backend.dto.response.EnR002FileResponse;
import com.kct.jcredit.core_backend.entity.ParticipatingProject;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;

import io.jsonwebtoken.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.kct.jcredit.core_backend.dto.request.EnR002ConfirmationItemRequest;
import com.kct.jcredit.core_backend.dto.request.EnR002JoinProjectRequest;

public interface EnR002FileUploadService {

    // Upload file evidence equipment ke S3 temp
    EnR002FileResponse uploadFile(String siteId,
                                        EquipmentFileType fileType,
                                        String uploadedBy,
                                        MultipartFile file);

    // Hitung key final dari key temp. Tidak menyentuh S3 sama sekali, supaya
    // key ini bisa disimpan ke DB di dalam transaksi — sebelum objectnya dipindah.
    // projectId dihapus — equipment cuma butuh siteId + equipmentId
    String buildPermanentKey(String tempS3Key,
                             String siteId,
                             Integer equipmentId);

    // Copy tmp → permanent lalu hapus tmp. Dipanggil setelah commit, memakai
    // key yang sudah tercatat di DB, jadi DB tidak pernah menunjuk ke tmp/.
    void moveToPermanent(String tempS3Key, String permanentKey);

    // Hapus object permanent. Object yang masih dirujuk equipment lain
    // dilewati, jadi file yang dipakai bersama tidak ikut terhapus.
    void deletePermanentFile(String s3Key, Integer excludeEquipmentId);
}
