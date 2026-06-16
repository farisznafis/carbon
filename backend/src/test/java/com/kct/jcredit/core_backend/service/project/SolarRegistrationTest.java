// package com.kct.jcredit.core_backend.service.project;

// import com.kct.jcredit.core_backend.constant.ResponseConstants;
// import com.kct.jcredit.core_backend.dto.request.SolarRegistrationRequest;
// import com.kct.jcredit.core_backend.dto.response.SolarRegistrationResponse;
// import com.kct.jcredit.core_backend.entity.*;
// import com.kct.jcredit.core_backend.exception.CustomException;
// import com.kct.jcredit.core_backend.repository.postgres.ProjectRepository;
// import com.kct.jcredit.core_backend.repository.postgres.SiteR002EquipmentFileRepository;
// import com.kct.jcredit.core_backend.repository.postgres.SiteR002MemberDataRepository;
// import com.kct.jcredit.core_backend.repository.postgres.SiteRepository;
// import com.kct.jcredit.core_backend.service.impl.SolarRegistrationServiceImpl;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.math.BigDecimal;
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyList;
// import static org.mockito.ArgumentMatchers.argThat;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// @DisplayName("SolarRegistrationService - Unit Tests")
// class SolarRegistrationTest {

//     @Mock
//     private ProjectRepository projectRepository;

//     @Mock
//     private SiteRepository siteRepository;

//     @Mock
//     private SiteR002MemberDataRepository memberDataRepository;

//     @Mock
//     private SiteR002EquipmentFileRepository equipmentFileRepository;

//     @InjectMocks
//     private SolarRegistrationServiceImpl registrationService;

//     // Data Dummy
//     private SolarRegistrationRequest validRequest;
//     private Site dummySite;
//     private Project dummyProject;
//     private ParticipatingCompany dummyCompany;

//     @BeforeEach
//     void setUp() {
//         Integer siteId = 123;
//         Integer projectId = 123;

//         // 1. Setup Company & Site (Master Data)
//         dummyCompany = new ParticipatingCompany();
//         dummyCompany.setRepLastName("山田");
//         dummyCompany.setRepFirstName("太郎");
        
//         dummySite = new Site();
//         dummySite.setId(siteId);
//         dummySite.setCompany(dummyCompany);

//         // 2. Setup Project 
//         dummyProject = new Project();
//         dummyProject.setId(projectId);
//         dummyProject.setCreatedAt(LocalDateTime.now().minusDays(10));

//         // 3. Setup Request DTO
//         validRequest = new SolarRegistrationRequest();
//         validRequest.setSiteId(siteId);
//         validRequest.setProjectId(projectId);
        
//         // Setup Equipment Item with Operation Date 1 year ago
//         SolarRegistrationRequest.EquipmentItem equipment = new SolarRegistrationRequest.EquipmentItem();
//         equipment.setDeviceNo("12345");
//         equipment.setEquipmentOpDate(LocalDate.now().minusYears(1));
//         equipment.setSubsidyAmount(new BigDecimal("420000")); // Ada subsidi
        
//         // Setup Dummy File data
//         SolarRegistrationRequest.EquipmentFile file = new SolarRegistrationRequest.EquipmentFile();
//         file.setFileId("doc_001");
//         file.setUrl("https://s3.dummy/doc.pdf");
//         equipment.setFiles(List.of(file));

//         validRequest.setEquipment(List.of(equipment));
//     }

//     @Test
//     @DisplayName("TC-SR-01: fails when already submitted (ALREADY_SUBMITTED)")
//     void processRegistration_fails_whenAlreadySubmitted() {
//         when(memberDataRepository.existsByProjectId(123)).thenReturn(true);

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             registrationService.processRegistration(validRequest);
//         });

//         assertEquals("ALREADY_SUBMITTED", exception.getErrorCode());
        
//         verify(siteRepository, never()).findById(any());
//     }

//     @Test
//     @DisplayName("TC-SR-02: fails when site not found (NOT_FOUND)")
//     void processRegistration_fails_whenSiteNotFound() {
//         when(memberDataRepository.existsByProjectId(123)).thenReturn(false);
//         when(siteRepository.findById(123)).thenReturn(Optional.empty());

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             registrationService.processRegistration(validRequest);
//         });

//         assertEquals(ResponseConstants.NOT_FOUND, exception.getErrorCode());
//     }

//     @Test
//     @DisplayName("TC-SR-03: fails when project not found (NOT_FOUND)")
//     void processRegistration_fails_whenProjectNotFound() {
//         when(memberDataRepository.existsByProjectId(123)).thenReturn(false);
//         when(siteRepository.findById(123)).thenReturn(Optional.of(dummySite));
//         when(projectRepository.findById(123)).thenReturn(Optional.empty());

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             registrationService.processRegistration(validRequest);
//         });

//         assertEquals(ResponseConstants.NOT_FOUND, exception.getErrorCode());
//     }

//     @Test
//     @DisplayName("TC-SR-04: fails when application date is before project date")
//     void processRegistration_fails_whenApplicationDateBeforeProjectDate() {
//         // Setting Project 
//         dummyProject.setCreatedAt(LocalDateTime.now().plusDays(1));
        
//         when(memberDataRepository.existsByProjectId(123)).thenReturn(false);
//         when(siteRepository.findById(123)).thenReturn(Optional.of(dummySite));
//         when(projectRepository.findById(123)).thenReturn(Optional.of(dummyProject));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             registrationService.processRegistration(validRequest);
//         });

//         assertEquals("VALIDATION_ERROR", exception.getErrorCode());
//         assertTrue(exception.getMessage().contains("入会申込日はプロジェクト登録日以降"));
//     }

//     @Test
//     @DisplayName("TC-SR-05: fails when equipment operation date is too old (more than 2 years ago)")
//     void processRegistration_fails_whenEquipmentOpDateTooOld() {
//         //if equipment operation date more than 2 years ago, should fail validation
//         validRequest.getEquipment().get(0).setEquipmentOpDate(LocalDate.now().minusYears(3));
        
//         when(memberDataRepository.existsByProjectId(123)).thenReturn(false);
//         when(siteRepository.findById(123)).thenReturn(Optional.of(dummySite));
//         when(projectRepository.findById(123)).thenReturn(Optional.of(dummyProject));

//         CustomException exception = assertThrows(CustomException.class, () -> {
//             registrationService.processRegistration(validRequest);
//         });

//         assertEquals("VALIDATION_ERROR", exception.getErrorCode());
//         assertTrue(exception.getMessage().contains("2年前以降"));
//     }

//     @Test
//     @DisplayName("TC-SR-06: successfully processes registration with valid data")
//     void processRegistration_success() {
//         when(memberDataRepository.existsByProjectId(123)).thenReturn(false);
//         when(siteRepository.findById(123)).thenReturn(Optional.of(dummySite));
//         when(projectRepository.findById(123)).thenReturn(Optional.of(dummyProject));
        
//         // Mock save member data
//         when(memberDataRepository.save(any(SiteR002MemberData.class))).thenAnswer(i -> i.getArgument(0));

//         SolarRegistrationResponse response = registrationService.processRegistration(validRequest);

//         assertEquals(ResponseConstants.OK, response.getStatusCode());
        
//         // (BigDecimal 420000 > 0 = true)
//         verify(memberDataRepository, times(1)).save(argThat(member -> member.getHasSubsidy() == true));
        
//         // save data to equipment file repository
//         verify(equipmentFileRepository, times(1)).saveAll(anyList());
//     }

//     @Test
//     @DisplayName("TC-SR-07: successfully processes registration with minimal data (no subsidy, no files, no operation date)")
//     void processRegistration_success_minimalData() {
//         // Given
//         when(memberDataRepository.existsByProjectId(123)).thenReturn(false);
        
//         // Site without company
//         Site dummySiteNoCompany = new Site();
//         dummySiteNoCompany.setId(123);
//         dummySiteNoCompany.setCompany(null); 
//         when(siteRepository.findById(123)).thenReturn(Optional.of(dummySiteNoCompany));
        
//         when(projectRepository.findById(123)).thenReturn(Optional.of(dummyProject));

//         // request with minimal data: subsidyAmount = null (hasSubsidy = false), equipmentOpDate = null, files = null
//         SolarRegistrationRequest minimalRequest = new SolarRegistrationRequest();
//         minimalRequest.setSiteId(123);
//         minimalRequest.setProjectId(123);
        
//         SolarRegistrationRequest.EquipmentItem minimalEquipment = new SolarRegistrationRequest.EquipmentItem();
//         minimalEquipment.setDeviceNo("99999");
//         minimalEquipment.setEquipmentOpDate(null); 
//         minimalEquipment.setSubsidyAmount(null); 
//         minimalEquipment.setFiles(null); 
        
//         minimalRequest.setEquipment(List.of(minimalEquipment));

//         // Mock save
//         when(memberDataRepository.save(any(SiteR002MemberData.class))).thenAnswer(i -> i.getArgument(0));

//         // When
//         SolarRegistrationResponse response = registrationService.processRegistration(minimalRequest);

//         // Then
//         assertEquals(ResponseConstants.OK, response.getStatusCode());
        
//         // Verifikasi hasSubsidy = false
//         verify(memberDataRepository, times(1)).save(argThat(member -> member.getHasSubsidy() == false));
        
//         // Verifikasi saveAll for equipment that has no files should not be called
//         verify(equipmentFileRepository, never()).saveAll(any());
//     }
// }
