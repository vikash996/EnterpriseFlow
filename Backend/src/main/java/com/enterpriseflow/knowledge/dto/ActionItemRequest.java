package com.enterpriseflow.knowledge.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate; import java.util.UUID;
public record ActionItemRequest(@NotBlank @Size(max=200) String title, UUID assigneeId, LocalDate dueDate) {}
