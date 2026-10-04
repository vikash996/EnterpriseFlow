package com.enterpriseflow.service;

import com.enterpriseflow.entity.Company;
import com.enterpriseflow.entity.MembershipStatus;
import com.enterpriseflow.entity.User;
import com.enterpriseflow.entity.UserRole;
import com.enterpriseflow.repository.ProjectRepository;
import com.enterpriseflow.repository.TaskRepository;
import com.enterpriseflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandCenterServiceTest {
    private static final String MEMBER_EMAIL = "member@example.com";

    @Mock private UserRepository users;
    @Mock private ProjectRepository projects;
    @Mock private TaskRepository tasks;
    @Mock private KnowledgeService knowledge;
    @InjectMocks private CommandCenterService service;

    @Test
    void memberIsDeniedBeforeCompanyDataOrInsightsAreRead() {
        User member = User.create("Member", MEMBER_EMAIL, "hash", UserRole.MEMBER,
                new Company("Member Company"), MembershipStatus.ACTIVE);
        when(users.findByEmail(MEMBER_EMAIL)).thenReturn(Optional.of(member));

        assertThrows(AccessDeniedException.class, () -> service.get(MEMBER_EMAIL));

        verify(projects, never()).findActiveByCompanyId(org.mockito.ArgumentMatchers.any());
        verify(tasks, never()).findByProjectCompanyId(org.mockito.ArgumentMatchers.any());
        verify(knowledge, never()).ask(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
