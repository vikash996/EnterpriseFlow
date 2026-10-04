package com.enterpriseflow.repository; import com.enterpriseflow.entity.Project; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface ProjectRepository extends JpaRepository<Project,UUID>{
 @Query("select distinct p from Project p left join fetch p.members left join fetch p.owner where p.archived=false and p.company.id=:companyId")
 List<Project> findActiveByCompanyId(UUID companyId);

 @Query("select distinct p from Project p left join fetch p.members left join fetch p.owner " +
        "where p.archived=false and p.company.id=:companyId and " +
        "(p.owner.id=:userId or exists (select m.id from Project memberProject join memberProject.members m " +
        "where memberProject=p and m.id=:userId))")
 List<Project> findVisibleByCompanyIdAndUserId(UUID companyId, UUID userId);
}
