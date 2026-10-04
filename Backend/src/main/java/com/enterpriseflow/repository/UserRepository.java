package com.enterpriseflow.repository;

import com.enterpriseflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import com.enterpriseflow.entity.UserRole;
import com.enterpriseflow.entity.MembershipStatus;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    long countByRole(UserRole role);
    boolean existsByRoleAndMembershipStatus(UserRole role, MembershipStatus membershipStatus);
    long countByCompanyIdAndRoleAndMembershipStatus(UUID companyId, UserRole role, MembershipStatus membershipStatus);
    java.util.List<User> findByCompanyIdAndMembershipStatusOrderByCreatedAtDesc(UUID companyId, MembershipStatus membershipStatus);
}
