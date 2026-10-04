package com.enterpriseflow.controller;
import com.enterpriseflow.api.ApiResponse;
import com.enterpriseflow.repository.CompanyRepository;
import com.enterpriseflow.repository.UserRepository;
import com.enterpriseflow.entity.UserRole;
import com.enterpriseflow.entity.MembershipStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/companies")
public class CompanyController {
  private final CompanyRepository companies; private final UserRepository users;
  public CompanyController(CompanyRepository companies, UserRepository users) { this.companies = companies; this.users = users; }
  @GetMapping public ApiResponse<List<Map<String,Object>>> list() { return ApiResponse.success("Companies retrieved.", companies.findAll().stream().map(c -> Map.<String,Object>of("id", c.getId(), "name", c.getName())).toList()); }
  @GetMapping("/bootstrap-status") public ApiResponse<Map<String, Boolean>> bootstrapStatus() { return ApiResponse.success("Bootstrap status retrieved.", Map.of("canCreateInitialCompany", !users.existsByRoleAndMembershipStatus(UserRole.ADMIN, MembershipStatus.ACTIVE))); }
}
