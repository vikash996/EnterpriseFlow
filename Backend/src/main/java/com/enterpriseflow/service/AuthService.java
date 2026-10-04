package com.enterpriseflow.service;

import com.enterpriseflow.auth.DuplicateEmailException;
import com.enterpriseflow.auth.InvalidCredentialsException;
import com.enterpriseflow.auth.dto.AuthResponse;
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
import com.enterpriseflow.entity.JoinRequest;
import com.enterpriseflow.entity.MembershipStatus;
import com.enterpriseflow.entity.Notification;
import com.enterpriseflow.auth.dto.CompanyRegistrationRequest;
import com.enterpriseflow.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CompanyRepository companyRepository;
    private final JoinRequestRepository joinRequests;
    private final NotificationRepository notifications;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, CompanyRepository companyRepository, JoinRequestRepository joinRequests, NotificationRepository notifications) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.companyRepository = companyRepository;
        this.joinRequests = joinRequests;
        this.notifications = notifications;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }

        if (request.companyId() == null) throw new IllegalArgumentException("Select a company to request access.");
        Company company = companyRepository.findById(request.companyId()).orElseThrow(() -> new IllegalArgumentException("Selected company does not exist."));
        User user = User.create(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserRole.MEMBER, company, MembershipStatus.PENDING
        );
        User saved = userRepository.save(user);
        joinRequests.save(new JoinRequest(saved, company));
        userRepository.findByCompanyIdAndMembershipStatusOrderByCreatedAtDesc(company.getId(), MembershipStatus.ACTIVE).stream()
                .filter(candidate -> candidate.getRole() == UserRole.ADMIN)
                .forEach(admin -> notifications.save(new Notification(admin, "New join request", saved.getName() + " wants to join " + company.getName() + ".", "JOIN_REQUEST")));
        return UserResponse.from(saved);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public UserResponse registerCompany(CompanyRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByRoleAndMembershipStatus(UserRole.ADMIN, MembershipStatus.ACTIVE)) {
            throw new IllegalArgumentException("An EnterpriseFlow company owner already exists. Request to join that company instead.");
        }
        if (userRepository.findByEmail(email).isPresent()) throw new DuplicateEmailException();
        Company company = companyRepository.save(new Company(request.companyName().trim()));
        User owner = User.create(request.ownerName().trim(), email, passwordEncoder.encode(request.password()), UserRole.ADMIN, company, MembershipStatus.ACTIVE);
        return UserResponse.from(userRepository.save(owner));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
