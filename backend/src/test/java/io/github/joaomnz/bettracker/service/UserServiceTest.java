package io.github.joaomnz.bettracker.service;

import io.github.joaomnz.bettracker.dto.user.*;
import io.github.joaomnz.bettracker.enums.OtpPurpose;
import io.github.joaomnz.bettracker.exception.BusinessRuleException;
import io.github.joaomnz.bettracker.exception.DataConflictException;
import io.github.joaomnz.bettracker.exception.ResourceNotFoundException;
import io.github.joaomnz.bettracker.factory.UserTestDataBuilder;
import io.github.joaomnz.bettracker.model.User;
import io.github.joaomnz.bettracker.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserService")
@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private OtpTokenService otpTokenService;
    @Spy private Clock clock = Clock.fixed(Instant.parse("2026-03-26T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("Get Profile")
    class GetProfileTests {
        @Test
        void shouldReturnUserProfileWhenUserExists(){
            Long id = 1L;
            String name = "Test User";
            String email = "test@email.com";

            User mockUser = new UserTestDataBuilder()
                    .withName(name)
                    .withEmail(email)
                    .build();

            when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

            UserProfileResponse response = userService.getProfile(id);

            assertThat(response.name()).isEqualTo(name);
            assertThat(response.email()).isEqualTo(email);
        }

        @Test
        void shouldThrowResourceNotFoundExceptionWhenUserDoesNotExist() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found.");
        }
    }

    @Nested
    @DisplayName("Update Profile")
    class UpdateProfileTests {
        @Test
        void shouldUpdateBothNameAndUnitValueWhenBothAreProvided(){
            Long id = 1L;
            String updatedName = "updated-name";
            BigDecimal updatedUnitValue = BigDecimal.TEN;

            User mockUser = new UserTestDataBuilder()
                    .withName("Test User")
                    .withEmail("test@email.com")
                    .withUnitValue(BigDecimal.ZERO)
                    .build();

            when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

            UserProfileResponse response = userService.updateProfile(id, new UpdateProfileRequest(updatedName, updatedUnitValue));

            assertThat(mockUser.getName()).isEqualTo(updatedName);
            assertThat(mockUser.getUnitValue()).isEqualTo(updatedUnitValue);

            assertThat(response.name()).isEqualTo(updatedName);
            assertThat(response.email()).isEqualTo("test@email.com");
        }

        @Test
        void shouldUpdateOnlyNameAndKeepUnitValueWhenUnitValueIsNull(){
            String updatedName = "updated-name";

            User mockUser = new UserTestDataBuilder()
                    .withName("Test User")
                    .withUnitValue(BigDecimal.ZERO)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            userService.updateProfile(1L, new UpdateProfileRequest(updatedName, null));

            assertThat(mockUser.getName()).isEqualTo(updatedName);
            assertThat(mockUser.getUnitValue()).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        void shouldUpdateOnlyUnitValueAndKeepNameWhenNameIsNull(){
            BigDecimal updatedUnitValue = BigDecimal.TEN;

            User mockUser = new UserTestDataBuilder()
                    .withName("Test User")
                    .withUnitValue(BigDecimal.ZERO)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            userService.updateProfile(1L, new UpdateProfileRequest(null, updatedUnitValue));

            assertThat(mockUser.getName()).isEqualTo("Test User");
            assertThat(mockUser.getUnitValue()).isEqualTo(updatedUnitValue);
        }
    }

    @Nested
    @DisplayName("Update Password")
    class UpdatePasswordTests {
        @Test
        void shouldEncodeNewPasswordAndSendNotificationWhenRequestIsValid() {
            Long id = 1L;
            String oldPassword = "Pass123!";
            String newPassword = "new-password";

            User mockUser = new UserTestDataBuilder()
                    .withName("Test User")
                    .withEmail("test@email.com")
                    .withPassword(oldPassword)
                    .build();

            when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

            when(passwordEncoder.matches(oldPassword, mockUser.getPassword())).thenReturn(true);
            when(passwordEncoder.matches(newPassword, mockUser.getPassword())).thenReturn(false);
            when(passwordEncoder.encode(newPassword)).thenReturn("encoded-password");

            userService.updatePassword(id, new UpdatePasswordRequest(oldPassword, newPassword));

            assertThat(mockUser.getPassword()).isEqualTo("encoded-password");

            verify(emailService).notifyPasswordChange("test@email.com", "Test User");
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenAccountHasNoPassword() {
            User mockUser = new UserTestDataBuilder()
                    .withPassword(null)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> userService.updatePassword(1L, new UpdatePasswordRequest("Pass123!", "new-password")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("This account does not have a password.");

            assertThat(mockUser.getPassword()).isNull();

            verifyNoInteractions(passwordEncoder, emailService);
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenCurrentPasswordIsIncorrect() {
            User mockUser = new UserTestDataBuilder()
                    .withPassword("Pass123!")
                    .build();
            String hashedMockUserPassword = mockUser.getPassword();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> userService.updatePassword(1L, new UpdatePasswordRequest("Pass123!", "new-password")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Incorrect current password.");

            assertThat(mockUser.getPassword()).isEqualTo(hashedMockUserPassword);

            verify(passwordEncoder, never()).encode(any());
            verifyNoInteractions(emailService);
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenNewPasswordMatchesCurrentOne() {
            User mockUser = new UserTestDataBuilder()
                    .withPassword("Pass123!")
                    .build();
            String hashedMockUserPassword = mockUser.getPassword();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            // Both matches() calls return true
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> userService.updatePassword(1L, new UpdatePasswordRequest("Pass123!", "new-password")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("New password cannot be the same as your current one.");

            assertThat(mockUser.getPassword()).isEqualTo(hashedMockUserPassword);

            verify(passwordEncoder, never()).encode(any());
            verifyNoInteractions(emailService);
        }
    }

    @Nested
    @DisplayName("Set Password")
    class SetPasswordTests {
        @Test
        void shouldEncodeAndSetPasswordAndSendNotificationWhenAccountHasNoPassword() {
            Long id = 1L;
            String newPassword = "new-password";

            User mockUser = new UserTestDataBuilder()
                    .withName("Test User")
                    .withEmail("test@email.com")
                    .withPassword(null)
                    .build();

            when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

            when(passwordEncoder.encode(newPassword)).thenReturn("encoded-password");

            userService.setPassword(id, new SetPasswordRequest(newPassword));

            assertThat(mockUser.getPassword()).isEqualTo("encoded-password");

            verify(emailService).notifyPasswordSet("test@email.com", "Test User");
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenAccountAlreadyHasPassword() {
            User mockUser = new UserTestDataBuilder()
                    .withPassword("Pass123!")
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> userService.setPassword(1L, new SetPasswordRequest("new-password")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("You already have a password.");

            verifyNoInteractions(passwordEncoder, emailService);
        }
    }

    @Nested
    @DisplayName("Request Email Change")
    class RequestEmailChangeTests {
        @Test
        void shouldNormalizeEmailSetPendingAndSendOtpWhenUserHasNoPassword() {
            Long id = 1L;
            String newEmail = "  TEST@Email.com  ";
            String expectedNormalizedEmail = "test@email.com";

            User mockUser = new UserTestDataBuilder()
                    .withName("Test User")
                    .withEmail("another-email")
                    .withPendingEmail(null)
                    .withPassword(null)
                    .build();

            when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

            when(userRepository.existsByEmail(expectedNormalizedEmail)).thenReturn(false);

            LocalDateTime expectedExpiry = LocalDateTime.now(clock).plusMinutes(15);
            when(otpTokenService.createOtp(same(mockUser), eq(OtpPurpose.EMAIL_CHANGE), eq(expectedExpiry))).thenReturn("123456");

            userService.requestEmailChange(id, new RequestEmailChangeRequest(newEmail, "Pass123!"));

            verifyNoInteractions(passwordEncoder);

            assertThat(mockUser.getPendingEmail()).isEqualTo(expectedNormalizedEmail);

            verify(emailService).sendEmailChangeCode(expectedNormalizedEmail, "Test User", "123456");
        }

        @Test
        void shouldSetPendingEmailAndSendOtpWhenPasswordMatches() {
            String password = "Pass123!";

            User mockUser = new UserTestDataBuilder()
                    .withEmail("another-email")
                    .withPassword(password)
                    .withPendingEmail(null)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            when(passwordEncoder.matches(password, mockUser.getPassword())).thenReturn(true);

            when(userRepository.existsByEmail(any())).thenReturn(false);

            when(otpTokenService.createOtp(any(), any(), any())).thenReturn("123456");

            userService.requestEmailChange(1L, new RequestEmailChangeRequest("test@email.com", password));
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenPasswordIsNullAndAccountHasPassword() {
            User mockUser = new UserTestDataBuilder()
                    .withPendingEmail(null)
                    .withPassword("Pass123!")
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> userService.requestEmailChange(1L, new RequestEmailChangeRequest("test@email.com", null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Incorrect password.");

            assertThat(mockUser.getPendingEmail()).isEqualTo(null);

            verifyNoInteractions(passwordEncoder, otpTokenService, emailService);
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenCurrentPasswordIsIncorrect() {
            String currentPassword = "Pass123!";

            User mockUser = new UserTestDataBuilder()
                    .withPendingEmail(null)
                    .withPassword("another-password")
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> userService.requestEmailChange(1L, new RequestEmailChangeRequest("test@email.com", currentPassword)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Incorrect password.");

            assertThat(mockUser.getPendingEmail()).isEqualTo(null);

            verifyNoInteractions(otpTokenService, emailService);
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenNewEmailMatchesCurrentEmail() {
            String email = "test@email.com";

            User mockUser = new UserTestDataBuilder()
                    .withEmail(email)
                    .withPendingEmail(null)
                    .withPassword(null)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> userService.requestEmailChange(1L, new RequestEmailChangeRequest(email, null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("The new email must be different from your current one.");

            assertThat(mockUser.getPendingEmail()).isEqualTo(null);

            verifyNoInteractions(otpTokenService, emailService);
        }

        @Test
        void shouldThrowDataConflictExceptionWhenNewEmailIsAlreadyRegistered() {
            User mockUser = new UserTestDataBuilder()
                    .withEmail("another-email")
                    .withPendingEmail(null)
                    .withPassword(null)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            when(userRepository.existsByEmail(any())).thenReturn(true);

            assertThatThrownBy(() -> userService.requestEmailChange(1L, new RequestEmailChangeRequest("test@email.com", null)))
                    .isInstanceOf(DataConflictException.class)
                    .hasMessage("The email address is already registered.");

            assertThat(mockUser.getPendingEmail()).isEqualTo(null);

            verifyNoInteractions(otpTokenService, emailService);
        }
    }

    @Nested
    @DisplayName("Verify Email Change")
    class VerifyEmailChangeTests {
        @Test
        void shouldUpdateEmailClearPendingAndMarkVerifiedWhenOtpIsValid() {
            Long id = 1L;
            String otp = "123456";

            User mockUser = new UserTestDataBuilder()
                    .withName("Test User")
                    .withEmail("test@email.com")
                    .withPendingEmail("another-email")
                    .withVerified(false)
                    .build();

            when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

            userService.verifyEmailChange(id, new VerifyEmailChangeRequest(otp));

            verify(otpTokenService).verifyOtp(same(mockUser), eq(otp), eq(OtpPurpose.EMAIL_CHANGE));

            assertThat(mockUser.getEmail()).isEqualTo("another-email");
            assertThat(mockUser.getPendingEmail()).isNull();
            assertThat(mockUser.isVerified()).isTrue();

            verify(emailService).notifyEmailChange("test@email.com", "Test User");
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenNoPendingEmailChangeExists() {
            User mockUser = new UserTestDataBuilder()
                    .withEmail("test@email.com")
                    .withPendingEmail(null)
                    .withVerified(false)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> userService.verifyEmailChange(1L, new VerifyEmailChangeRequest("123456")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("No email change request is currently pending.");

            assertThat(mockUser.getEmail()).isEqualTo("test@email.com");
            assertThat(mockUser.getPendingEmail()).isNull();
            assertThat(mockUser.isVerified()).isFalse();

            verifyNoInteractions(otpTokenService, emailService);
        }
    }

    @Nested
    @DisplayName("Deactivate Account")
    class DeactivateAccountTests {
        @Test
        void shouldDeactivateAccountAndSendNotificationWhenUserHasNoPassword() {
            Long id = 1L;

            User mockUser = new UserTestDataBuilder()
                    .withName("Test User")
                    .withEmail("test@email.com")
                    .withPassword(null)
                    .withActive(true)
                    .build();

            when(userRepository.findById(id)).thenReturn(Optional.of(mockUser));

            userService.deactivateAccount(id, new DeactivateAccountRequest("Pass123!"));

            verifyNoInteractions(passwordEncoder);

            assertThat(mockUser.isActive()).isFalse();

            verify(emailService).notifyAccountDeactivation("test@email.com", "Test User");
        }

        @Test
        void shouldDeactivateAccountAndSendNotificationWhenPasswordMatches() {
            String password = "Pass123!";

            User mockUser = new UserTestDataBuilder()
                    .withPassword(password)
                    .withActive(true)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            when(passwordEncoder.matches(password, mockUser.getPassword())).thenReturn(true);

            userService.deactivateAccount(1L, new DeactivateAccountRequest(password));
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenAccountIsAlreadyInactive() {
            User mockUser = new UserTestDataBuilder()
                    .withActive(false)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> userService.deactivateAccount(1L, new DeactivateAccountRequest("Pass123!")))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Account is already deactivated.");

            assertThat(mockUser.isActive()).isFalse();

            verifyNoInteractions(passwordEncoder, emailService);
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenPasswordIsNullAndAccountHasPassword() {
            User mockUser = new UserTestDataBuilder()
                    .withPassword("Pass123!")
                    .withActive(true)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            assertThatThrownBy(() -> userService.deactivateAccount(1L, new DeactivateAccountRequest(null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Incorrect password.");

            assertThat(mockUser.isActive()).isTrue();

            verifyNoInteractions(passwordEncoder, emailService);
        }

        @Test
        void shouldThrowBusinessRuleExceptionWhenPasswordIsIncorrect() {
            String password = "another-password";

            User mockUser = new UserTestDataBuilder()
                    .withPassword("Pass123!")
                    .withActive(true)
                    .build();

            when(userRepository.findById(any())).thenReturn(Optional.of(mockUser));

            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> userService.deactivateAccount(1L, new DeactivateAccountRequest(password)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Incorrect password.");

            assertThat(mockUser.isActive()).isTrue();

            verifyNoInteractions(emailService);
        }
    }
}