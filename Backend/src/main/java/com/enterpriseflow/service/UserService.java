package com.enterpriseflow.service;

import com.enterpriseflow.auth.dto.UserResponse;
import com.enterpriseflow.entity.User;
import com.enterpriseflow.entity.UserRole;
import com.enterpriseflow.entity.ActivityLog;
import com.enterpriseflow.repository.ActivityLogRepository;
import com.enterpriseflow.repository.UserRepository;
import com.enterpriseflow.user.UserNotFoundException;
import com.enterpriseflow.user.dto.UpdateProfileRequest;
import com.enterpriseflow.entity.MembershipStatus;
import com.enterpriseflow.entity.JoinRequest;
import com.enterpriseflow.entity.Notification;
import com.enterpriseflow.repository.JoinRequestRepository;
import com.enterpriseflow.repository.NotificationRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final NotificationRepository notificationRepository;

    public UserService(UserRepository userRepository, ActivityLogRepository activityLogRepository, JoinRequestRepository joinRequestRepository, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return UserResponse.from(findByEmail(email));
    }

    @Transactional
    public UserResponse updateCurrentUser(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        user.updateName(request.name().trim());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers(String actorEmail) {
        User actor = findActiveAdmin(actorEmail);
        return userRepository.findByCompanyIdAndMembershipStatusOrderByCreatedAtDesc(actor.getCompany().getId(), MembershipStatus.ACTIVE)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(String actorEmail, UUID id) {
        User actor = findActiveAdmin(actorEmail); User target = findById(id);
        if (target.getCompany() == null || !target.getCompany().getId().equals(actor.getCompany().getId())) throw new org.springframework.security.access.AccessDeniedException("User belongs to another company.");
        return UserResponse.from(target);
    }

    @Transactional
    public UserResponse updateUserRole(String actorEmail, UUID id, UserRole role) {
        User actor = findActiveAdmin(actorEmail);
        User user = findById(id);
        if (user.getCompany() == null || !user.getCompany().getId().equals(actor.getCompany().getId()) || user.getMembershipStatus() != MembershipStatus.ACTIVE) throw new org.springframework.security.access.AccessDeniedException("User is not an approved member of your company.");
        if (user.getRole() == UserRole.ADMIN && role == UserRole.MEMBER && userRepository.countByCompanyIdAndRoleAndMembershipStatus(actor.getCompany().getId(), UserRole.ADMIN, MembershipStatus.ACTIVE) <= 1) {
            throw new IllegalArgumentException("At least one administrator must remain.");
        }
        user.updateRole(role);
        User updated = userRepository.save(user);
        activityLogRepository.save(new ActivityLog(actor, "UPDATED_ROLE", "USER", updated.getId(), "Updated " + updated.getName() + " to " + role));
        return UserResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> pendingRequests(String actorEmail) {
        User actor = findActiveAdmin(actorEmail);
        return joinRequestRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(actor.getCompany().getId(), MembershipStatus.PENDING).stream()
                .map(r -> java.util.Map.<String, Object>of("id", r.getId(), "userId", r.getUser().getId(), "name", r.getUser().getName(), "email", r.getUser().getEmail(), "createdAt", r.getCreatedAt())).toList();
    }

    @Transactional
    public UserResponse decideJoinRequest(String actorEmail, UUID requestId, boolean approve) {
        User actor = findActiveAdmin(actorEmail);
        JoinRequest request = joinRequestRepository.findById(requestId).orElseThrow(() -> new UserNotFoundException(requestId));
        if (!request.getCompany().getId().equals(actor.getCompany().getId())) throw new org.springframework.security.access.AccessDeniedException("Join request belongs to another company.");
        if (request.getStatus() != MembershipStatus.PENDING) throw new IllegalArgumentException("This join request has already been decided.");
        MembershipStatus status = approve ? MembershipStatus.ACTIVE : MembershipStatus.REJECTED;
        request.decide(status); request.getUser().updateMembershipStatus(status);
        notificationRepository.save(new Notification(request.getUser(), approve ? "Join request approved" : "Join request rejected", approve ? "You can now access " + actor.getCompany().getName() + "." : "Your request to join " + actor.getCompany().getName() + " was rejected.", "JOIN_REQUEST_" + status));
        activityLogRepository.save(new ActivityLog(actor, approve ? "APPROVED_JOIN" : "REJECTED_JOIN", "USER", request.getUser().getId(), (approve ? "Approved " : "Rejected ") + request.getUser().getName()));
        return UserResponse.from(request.getUser());
    }

    private User findActiveAdmin(String email) {
        User user = findByEmail(email);
        if (user.getRole() != UserRole.ADMIN || user.getMembershipStatus() != MembershipStatus.ACTIVE || user.getCompany() == null) throw new org.springframework.security.access.AccessDeniedException("Administrator access is required.");
        return user;
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    }

    private User findById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
