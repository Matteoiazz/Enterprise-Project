package com.tripify.user_auth_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.tripify.user_auth_service.dto.request.CompanionDto;
import com.tripify.user_auth_service.dto.request.PaymentMethodDto;
import com.tripify.user_auth_service.dto.request.TravelDocumentDto;
import com.tripify.user_auth_service.dto.request.UpdateProfileRequestDTO;
import com.tripify.user_auth_service.dto.response.UserResponse;
import com.tripify.user_auth_service.entity.Companion;
import com.tripify.user_auth_service.entity.PaymentMethod;
import com.tripify.user_auth_service.entity.Role;
import com.tripify.user_auth_service.entity.TravelDocument;
import com.tripify.user_auth_service.entity.User;
import com.tripify.user_auth_service.exception.ResourceNotFoundException;
import com.tripify.user_auth_service.exception.UnauthorizedActionException;
import com.tripify.user_auth_service.repository.CompanionRepository;
import com.tripify.user_auth_service.repository.PaymentMethodRepository;
import com.tripify.user_auth_service.repository.TravelDocumentRepository;
import com.tripify.user_auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock UserRepository userRepository;
    @Mock CompanionRepository companionRepository;
    @Mock PaymentMethodRepository paymentMethodRepository;
    @Mock TravelDocumentRepository documentRepository;
    @Mock Cloudinary cloudinary;

    @InjectMocks ProfileService service;

    private static final String EMAIL = "mario@test.com";

    @BeforeEach
    void deadKeycloak() {
        ReflectionTestUtils.setField(service, "keycloakAdminServerUrl", "http://127.0.0.1:1");
        ReflectionTestUtils.setField(service, "keycloakAdminUsername", "admin");
        ReflectionTestUtils.setField(service, "keycloakAdminPassword", "admin");
    }

    private User freshUser(Role role) {
        User u = User.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .password("x")
                .role(role)
                .username(UUID.randomUUID().toString())
                .name("Mario")
                .surname("Rossi")
                .phone("+39 333 1234567")
                .lastSyncedAt(Instant.now())
                .build();
        return u;
    }

    @Test
    void getUser_returnsExistingFreshUser_withoutContactingKeycloak() {
        User u = freshUser(Role.ROLE_ORGANIZER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));

        User result = service.getUser(EMAIL);

        assertThat(result).isSameAs(u);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUser_whenSyncedLongAgo_stampsLastSyncedAndSaves() {
        User u = freshUser(Role.ROLE_ORGANIZER);
        u.setLastSyncedAt(Instant.now().minusSeconds(3600));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.getUser(EMAIL);

        assertThat(result.getLastSyncedAt()).isAfter(Instant.now().minusSeconds(5));
        verify(userRepository).save(u);
    }

    @Test
    void getUser_whenMissing_provisionsTravelerFromKeycloakFallback() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.getUser(EMAIL);

        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getRole()).isEqualTo(Role.ROLE_TRAVELER);
        assertThat(result.getLastSyncedAt()).isNotNull();
    }

    @Test
    void addCompanion_rejectsBlankFirstName() {
        CompanionDto dto = CompanionDto.builder().firstName(" ").lastName("Rossi").dateOfBirth(LocalDate.of(1990, 1, 1)).build();

        assertThatThrownBy(() -> service.addCompanion(EMAIL, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome del compagno");
        verify(companionRepository, never()).save(any());
    }

    @Test
    void addCompanion_rejectsFutureDateOfBirth() {
        CompanionDto dto = CompanionDto.builder().firstName("Luca").lastName("Verdi")
                .dateOfBirth(LocalDate.now().plusDays(1)).build();

        assertThatThrownBy(() -> service.addCompanion(EMAIL, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void addCompanion_rejectsMinor() {
        CompanionDto dto = CompanionDto.builder().firstName("Luca").lastName("Verdi")
                .dateOfBirth(LocalDate.now().minusYears(10)).build();

        assertThatThrownBy(() -> service.addCompanion(EMAIL, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maggiorenne");
    }

    @Test
    void addCompanion_savesValidCompanion() {
        User u = freshUser(Role.ROLE_TRAVELER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        CompanionDto dto = CompanionDto.builder().firstName("Luca").lastName("Verdi")
                .dateOfBirth(LocalDate.of(1985, 6, 15)).build();
        Companion saved = Companion.builder().id(UUID.randomUUID())
                .firstName("Luca").lastName("Verdi").dateOfBirth(dto.getDateOfBirth()).user(u).build();
        when(companionRepository.save(any(Companion.class))).thenReturn(saved);

        CompanionDto result = service.addCompanion(EMAIL, dto);

        assertThat(result.getFirstName()).isEqualTo("Luca");
        assertThat(result.getId()).isEqualTo(saved.getId());
    }

    @Test
    void deleteCompanion_whenNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(companionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCompanion(EMAIL, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCompanion_whenNotOwner_throwsUnauthorized() {
        User owner = freshUser(Role.ROLE_TRAVELER);
        User other = freshUser(Role.ROLE_TRAVELER);
        UUID id = UUID.randomUUID();
        Companion companion = Companion.builder().id(id).user(other).build();
        when(companionRepository.findById(id)).thenReturn(Optional.of(companion));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.deleteCompanion(EMAIL, id))
                .isInstanceOf(UnauthorizedActionException.class);
        verify(companionRepository, never()).delete(any());
    }

    @Test
    void addTravelDocument_rejectsExpiredDocument() {
        TravelDocumentDto dto = TravelDocumentDto.builder()
                .documentType("PASSPORT").documentNumber("AB123").issuingCountry("IT")
                .expirationDate(LocalDate.now().minusDays(1)).build();

        assertThatThrownBy(() -> service.addTravelDocument(EMAIL, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scaduto");
    }

    @Test
    void addTravelDocument_savesValidDocument() {
        User u = freshUser(Role.ROLE_TRAVELER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        TravelDocumentDto dto = TravelDocumentDto.builder()
                .documentType("PASSPORT").documentNumber("AB123").issuingCountry("IT")
                .expirationDate(LocalDate.now().plusYears(3)).build();
        TravelDocument entity = TravelDocument.builder().id(UUID.randomUUID()).user(u)
                .documentType("PASSPORT").documentNumber("AB123").issuingCountry("IT")
                .expirationDate(dto.getExpirationDate()).build();
        when(documentRepository.save(any(TravelDocument.class))).thenReturn(entity);

        TravelDocumentDto result = service.addTravelDocument(EMAIL, dto);

        assertThat(result.getId()).isEqualTo(entity.getId());
    }

    @Test
    void addPaymentMethod_rejectsInvalidCardNumber() {
        PaymentMethodDto dto = PaymentMethodDto.builder()
                .cardNumber("abcd").expirationMonthYear("12/30").cardProvider("VISA").build();

        assertThatThrownBy(() -> service.addPaymentMethod(EMAIL, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Numero carta");
    }

    @Test
    void addPaymentMethod_rejectsExpiredCard() {
        PaymentMethodDto dto = PaymentMethodDto.builder()
                .cardNumber("4111111111111111").expirationMonthYear("01/20").cardProvider("VISA").build();

        assertThatThrownBy(() -> service.addPaymentMethod(EMAIL, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scaduta");
    }

    @Test
    void addPaymentMethod_rejectsMalformedExpiry() {
        PaymentMethodDto dto = PaymentMethodDto.builder()
                .cardNumber("4111111111111111").expirationMonthYear("2030-01").cardProvider("VISA").build();

        assertThatThrownBy(() -> service.addPaymentMethod(EMAIL, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato scadenza");
    }

    @Test
    void addPaymentMethod_storesOnlyLastFourDigits() {
        User u = freshUser(Role.ROLE_TRAVELER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenAnswer(inv -> inv.getArgument(0));
        PaymentMethodDto dto = PaymentMethodDto.builder()
                .cardNumber("4111 1111 1111 1111").expirationMonthYear("12/30").cardProvider("VISA").build();

        PaymentMethodDto result = service.addPaymentMethod(EMAIL, dto);

        assertThat(result.getLastFourDigits()).isEqualTo("1111");
        ArgumentCaptor<PaymentMethod> captor = ArgumentCaptor.forClass(PaymentMethod.class);
        verify(paymentMethodRepository).save(captor.capture());
        assertThat(captor.getValue().getLastFourDigits()).isEqualTo("1111");
    }

    @Test
    void updatePec_whenNotOrganizer_throwsUnauthorized() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(freshUser(Role.ROLE_TRAVELER)));

        assertThatThrownBy(() -> service.updatePec(EMAIL, "pec@pec.it"))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void updatePec_whenFormatInvalid_throwsIllegalArgument() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(freshUser(Role.ROLE_ORGANIZER)));

        assertThatThrownBy(() -> service.updatePec(EMAIL, "non-una-pec"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PEC non valida");
    }

    @Test
    void updatePec_savesTrimmedValueForOrganizer() {
        User u = freshUser(Role.ROLE_ORGANIZER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));

        service.updatePec(EMAIL, "  pec@pec.it  ");

        assertThat(u.getPec()).isEqualTo("pec@pec.it");
        verify(userRepository).save(u);
    }

    @Test
    void updateUserProfile_whenNewEmailAlreadyUsed_throwsIllegalArgument() {
        User u = freshUser(Role.ROLE_TRAVELER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        when(userRepository.existsByEmail("preso@test.com")).thenReturn(true);
        UpdateProfileRequestDTO req = new UpdateProfileRequestDTO();
        req.setEmail("preso@test.com");

        assertThatThrownBy(() -> service.updateUserProfile(EMAIL, "kc-id", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("già in uso");
        verify(userRepository, never()).save(any());
    }

    @Test
    void uploadProfilePicture_rejectsNonImage() {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThatThrownBy(() -> service.uploadProfilePicture(EMAIL, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("immagine");
    }

    @Test
    void uploadProfilePicture_storesSecureHttpsUrl() throws Exception {
        User u = freshUser(Role.ROLE_TRAVELER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getBytes()).thenReturn(new byte[]{1, 2, 3});
        Uploader uploader = org.mockito.Mockito.mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), any())).thenReturn(Map.of(
                "url", "http://res.cloudinary.com/demo/image/upload/x.jpg",
                "secure_url", "https://res.cloudinary.com/demo/image/upload/x.jpg"));

        String url = service.uploadProfilePicture(EMAIL, file);

        assertThat(url).startsWith("https://");
        assertThat(u.getProfilePictureUrl()).isEqualTo(url);
    }

    @Test
    void normalizeImageUrl_upgradesHttpCloudinaryToHttps() {
        assertThat(ProfileService.normalizeImageUrl("http://res.cloudinary.com/demo/x.jpg"))
                .isEqualTo("https://res.cloudinary.com/demo/x.jpg");
    }

    @Test
    void normalizeImageUrl_leavesHttpsAndNonCloudinaryUntouched() {
        assertThat(ProfileService.normalizeImageUrl("https://res.cloudinary.com/demo/x.jpg"))
                .isEqualTo("https://res.cloudinary.com/demo/x.jpg");
        assertThat(ProfileService.normalizeImageUrl("http://example.com/x.jpg"))
                .isEqualTo("http://example.com/x.jpg");
        assertThat(ProfileService.normalizeImageUrl(null)).isNull();
    }

    @Test
    void getUserSummary_doesNotExposeEmail() {
        User u = freshUser(Role.ROLE_TRAVELER);
        String sub = u.getUsername();
        when(userRepository.findByUsername(sub)).thenReturn(Optional.of(u));

        UserResponse response = service.getUserSummary(sub);

        assertThat(response.email()).isNull();
        assertThat(response.name()).isEqualTo("Mario");
    }

    @Test
    void getOrganizerById_exposesEmailAndCompany() {
        User u = freshUser(Role.ROLE_ORGANIZER);
        u.setCompanyName("Tripify SRL");
        String sub = u.getUsername();
        when(userRepository.findByUsername(sub)).thenReturn(Optional.of(u));

        UserResponse response = service.getOrganizerById(sub);

        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.companyName()).isEqualTo("Tripify SRL");
    }

    @Test
    void getOrganizerById_whenUserIsNotOrganizer_throwsResourceNotFound() {
        User u = freshUser(Role.ROLE_TRAVELER);
        String sub = u.getUsername();
        when(userRepository.findByUsername(sub)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.getOrganizerById(sub))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllOrganizers_mapsEveryOrganizerWithEmail() {
        User a = freshUser(Role.ROLE_ORGANIZER);
        User b = freshUser(Role.ROLE_ORGANIZER);
        when(userRepository.findByRole(Role.ROLE_ORGANIZER)).thenReturn(List.of(a, b));

        List<UserResponse> result = service.getAllOrganizers();

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(r -> assertThat(r.email()).isNotNull());
    }
}
