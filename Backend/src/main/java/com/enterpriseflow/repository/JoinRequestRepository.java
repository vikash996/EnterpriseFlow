package com.enterpriseflow.repository;
import com.enterpriseflow.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface JoinRequestRepository extends JpaRepository<JoinRequest, UUID> {
    List<JoinRequest> findByCompanyIdAndStatusOrderByCreatedAtDesc(UUID companyId, MembershipStatus status);
}
