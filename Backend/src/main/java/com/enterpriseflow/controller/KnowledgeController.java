package com.enterpriseflow.controller;

import com.enterpriseflow.api.ApiResponse;
import com.enterpriseflow.knowledge.dto.ActionItemRequest;
import com.enterpriseflow.knowledge.dto.MeetingRequest;
import com.enterpriseflow.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Authorized document knowledge and meeting collaboration endpoints. */
@RestController
@RequestMapping("/api")
public class KnowledgeController {
    private final KnowledgeService knowledge;

    public KnowledgeController(KnowledgeService knowledge) { this.knowledge = knowledge; }

    @GetMapping("/documents")
    public ApiResponse<List<Map<String, Object>>> documents(Authentication auth) {
        return ApiResponse.success("Documents retrieved.", knowledge.documents(auth.getName()));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(Authentication auth,
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = "COMPANY") String visibility,
            @RequestParam(required = false) UUID projectId,
            @RequestPart("file") MultipartFile file) throws Exception {
        return ApiResponse.success("Document uploaded.", knowledge.upload(auth.getName(), title, visibility, projectId, file));
    }

    @GetMapping("/documents/{id}")
    public ApiResponse<Map<String, Object>> document(Authentication auth, @PathVariable UUID id) {
        return ApiResponse.success("Document retrieved.", knowledge.document(auth.getName(), id));
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(Authentication auth, @PathVariable UUID id) {
        Map<String, Object> metadata = knowledge.document(auth.getName(), id);
        Resource resource = knowledge.download(auth.getName(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.get("fileName").toString().replace("\"", "") + "\"")
                .contentType(MediaType.parseMediaType(metadata.get("contentType").toString().isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : metadata.get("contentType").toString()))
                .body(resource);
    }

    @DeleteMapping("/documents/{id}")
    public ApiResponse<Void> archive(Authentication auth, @PathVariable UUID id) {
        knowledge.archiveDocument(auth.getName(), id);
        return ApiResponse.success("Document archived.", null);
    }

    @PostMapping("/ai/ask")
    public ApiResponse<Map<String, Object>> ask(Authentication auth, @RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) throw new IllegalArgumentException("A question is required.");
        return ApiResponse.success("Knowledge search completed.", knowledge.ask(auth.getName(), question));
    }

    @GetMapping("/meetings")
    public ApiResponse<List<Map<String, Object>>> meetings(Authentication auth) {
        return ApiResponse.success("Meetings retrieved.", knowledge.meetings(auth.getName()));
    }

    @PostMapping("/meetings")
    public ApiResponse<Map<String, Object>> createMeeting(Authentication auth, @Valid @RequestBody MeetingRequest request) {
        return ApiResponse.success("Meeting created.", knowledge.createMeeting(auth.getName(), request));
    }

    @PostMapping("/meetings/{id}/summarize")
    public ApiResponse<Map<String, Object>> summarize(Authentication auth, @PathVariable UUID id) {
        return ApiResponse.success("Meeting summarized.", knowledge.summarize(auth.getName(), id));
    }

    @PostMapping("/meetings/{id}/action-items")
    public ApiResponse<Map<String, Object>> addActionItem(Authentication auth, @PathVariable UUID id, @Valid @RequestBody ActionItemRequest request) {
        return ApiResponse.success("Action item created.", knowledge.addActionItem(auth.getName(), id, request));
    }

    @PostMapping("/meetings/{meetingId}/action-items/{itemId}/convert")
    public ApiResponse<Map<String, Object>> convert(Authentication auth, @PathVariable UUID meetingId, @PathVariable UUID itemId) {
        return ApiResponse.success("Action item converted to task.", knowledge.convertActionItem(auth.getName(), meetingId, itemId));
    }
}
