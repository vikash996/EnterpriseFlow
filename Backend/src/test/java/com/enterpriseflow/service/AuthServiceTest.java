package com.enterpriseflow.service;

import com.enterpriseflow.auth.DuplicateEmailException;
import com.enterpriseflow.auth.InvalidCredentialsException;
import com.enterpriseflow.auth.dto.LoginRequest;
import com.enterpriseflow.auth.dto.RegisterRequest;
import com.enterpriseflow.auth.dto.UserResponse;
import com.enterpriseflow.entity.User;
import com.enterpriseflow.entity.UserRole;
import com.enterpriseflow.repository.UserRepository;
import com.enterpriseflow.repository.CompanyRepository;
import com.enterpriseflow.repository.JoinRequestRepository;
import com.enterpriseflow.repository.NotificationRepository;
import com.enterpriseflow.entity.Company;
import com.enterpriseflow.auth.dto.CompanyRegistrationRequest;
import com.enterpriseflow.entity.MembershipStatus;
import com.enterpriseflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;
    @Mock private CompanyRepository companyRepository;
    @Mock private JoinRequestRepository joinRequestRepository;
    @Mock private NotificationRepository notificationRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, companyRepository, joinRequestRepository, notificationRepository);
    }

    @Test
    void registrationHashesPasswordAndAssignsMemberRole() {
        UUID companyId = UUID.randomUUID(); RegisterRequest request = new RegisterRequest("Enterprise User", "USER@example.com", "secure-password", companyId);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(new Company("Enterprise")));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(request.password());
        assertThat(passwordEncoder.matches(request.password(), savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getRole()).isEqualTo(UserRole.MEMBER);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo(UserRole.MEMBER);
    }

    @Test
    void rejectsDuplicateEmailDuringRegistration() {
        RegisterRequest request = new RegisterRequest("Enterprise User", "user@example.com", "secure-password", UUID.randomUUID());
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(User.create("Existing User", "user@example.com", "hash", UserRole.MEMBER)));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsInvalidLoginCredentials() {
        User user = User.create(
                "Enterprise User",
                "user@example.com",
                passwordEncoder.encode("correct-password"),
                UserRole.MEMBER
        );
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void createsTheInitialCompanyOwnerAsAnActiveAdministrator() {
        when(userRepository.existsByRoleAndMembershipStatus(UserRole.ADMIN, MembershipStatus.ACTIVE)).thenReturn(false);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.registerCompany(new CompanyRegistrationRequest("Acme", "Company Owner", "owner@example.com", "secure-password"));

        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        assertThat(response.membershipStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void refusesAnotherCompanyBootstrapAfterAnAdministratorExists() {
        when(userRepository.existsByRoleAndMembershipStatus(UserRole.ADMIN, MembershipStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCompany(new CompanyRegistrationRequest("Other", "Other Owner", "other@example.com", "secure-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner already exists");
        verify(companyRepository, never()).save(any());
    }
}
