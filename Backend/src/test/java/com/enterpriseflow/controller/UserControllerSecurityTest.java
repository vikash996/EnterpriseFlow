package com.enterpriseflow.controller;

import com.enterpriseflow.auth.dto.UserResponse;
import com.enterpriseflow.entity.UserRole;
import com.enterpriseflow.repository.UserRepository;
import com.enterpriseflow.repository.ActivityLogRepository;
import com.enterpriseflow.security.JwtAuthenticationFilter;
import com.enterpriseflow.security.JwtService;
import com.enterpriseflow.security.RestAccessDeniedHandler;
import com.enterpriseflow.security.RestAuthenticationEntryPoint;
import com.enterpriseflow.config.SecurityConfig;
import com.enterpriseflow.service.AuthService;
import com.enterpriseflow.service.UserService;
import com.enterpriseflow.service.WorkspaceService;
import com.enterpriseflow.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, AuthController.class, AdminController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-that-is-longer-than-thirty-two-characters",
        "security.jwt.expiration-ms=3600000",
        "security.cors.allowed-origin=http://localhost:3000"
})
class UserControllerSecurityTest {

    private static final String MEMBER_EMAIL = "member@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final UUID USER_ID = UUID.fromString("1c6d4a60-73b4-4d6f-9a4e-e6ea7b219a6a");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ActivityLogRepository activityLogRepository;

    @MockBean
    private WorkspaceService workspaceService;

    @MockBean
    private NotificationService notificationService;

    @Test
    void currentUserEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getCurrentUser(any());
    }

    @Test
    @WithMockUser(username = MEMBER_EMAIL, roles = "MEMBER")
    void memberCanReadAndUpdateOwnProfileWithoutExposingPasswordHash() throws Exception {
        UserResponse user = new UserResponse(USER_ID, "Member User", MEMBER_EMAIL, UserRole.MEMBER, null);
        when(userService.getCurrentUser(MEMBER_EMAIL)).thenReturn(user);
        when(userService.updateCurrentUser(eq(MEMBER_EMAIL), any())).thenReturn(user);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(MEMBER_EMAIL))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Member\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Member User"));

        verify(userService).updateCurrentUser(eq(MEMBER_EMAIL), any());
    }

    @Test
    @WithMockUser(username = MEMBER_EMAIL, roles = "MEMBER")
    void memberCannotAccessAdminUserOperations() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/users/{id}/role", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers(any());
        verify(userService, never()).updateUserRole(any(), any(), any());
    }

    @Test
    @WithMockUser(username = MEMBER_EMAIL, roles = "MEMBER")
    void memberCannotAccessCompanyAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isForbidden());

        verify(workspaceService, never()).companyOverview(any());
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
    void adminCanListUsersAndUpdateRoles() throws Exception {
        UserResponse user = new UserResponse(USER_ID, "Member User", MEMBER_EMAIL, UserRole.MEMBER, null);
        when(userService.getAllUsers(ADMIN_EMAIL)).thenReturn(List.of(user));
        when(userService.updateUserRole(ADMIN_EMAIL, USER_ID, UserRole.ADMIN))
                .thenReturn(new UserResponse(USER_ID, "Member User", MEMBER_EMAIL, UserRole.ADMIN, null));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value(MEMBER_EMAIL));
        mockMvc.perform(patch("/api/users/{id}/role", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        verify(userService).updateUserRole(ADMIN_EMAIL, USER_ID, UserRole.ADMIN);
    }

    @Test
    @WithMockUser(username = ADMIN_EMAIL, roles = "ADMIN")
    void adminCanSeePendingJoinRequests() throws Exception {
        when(userService.pendingRequests(ADMIN_EMAIL)).thenReturn(List.of(Map.of(
                "id", USER_ID,
                "name", "Pending Member",
                "email", MEMBER_EMAIL
        )));

        mockMvc.perform(get("/api/users/join-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value(MEMBER_EMAIL));

        verify(userService).pendingRequests(ADMIN_EMAIL);
    }

    @Test
    void registrationCannotUseClientSuppliedAdminRole() throws Exception {
        UserResponse registeredUser = new UserResponse(USER_ID, "New Member", "new@example.com", UserRole.MEMBER, null);
        when(authService.register(any())).thenReturn(registeredUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Member\",\"email\":\"new@example.com\",\"password\":\"secure-password\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("MEMBER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }
}
