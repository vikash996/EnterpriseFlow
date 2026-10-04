package com.enterpriseflow.service;

import com.enterpriseflow.entity.*;
import com.enterpriseflow.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** Builds project context from existing records, after applying project and company access. */
@Service
public class ProjectKnowledgeService {
    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final MeetingRepository meetings;
    private final DocumentRepository documents;
    private final ActivityLogRepository activities;
    private final MeetingActionItemRepository actionItems;
    private final WorkInsightsService insights;

    public ProjectKnowledgeService(UserRepository users, ProjectRepository projects, TaskRepository tasks,
            MeetingRepository meetings, DocumentRepository documents, ActivityLogRepository activities,
            MeetingActionItemRepository actionItems, WorkInsightsService insights) {
        this.users = users; this.projects = projects; this.tasks = tasks; this.meetings = meetings;
        this.documents = documents; this.activities = activities; this.actionItems = actionItems; this.insights = insights;
    }

    private User actor(String email) {
        User user = users.findByEmail(email).orElseThrow();
        if (user.getMembershipStatus() != MembershipStatus.ACTIVE || user.getCompany() == null)
            throw new AccessDeniedException("Your company access is not active.");
        return user;
    }

    private Project authorizedProject(String email, UUID id) {
        User user = actor(email);
        Project project = projects.findById(id).orElseThrow(() -> new NoSuchElementException("Project not found."));
        if (project.isArchived() || project.getCompany() == null || !project.getCompany().getId().equals(user.getCompany().getId())
                || (user.getRole() != UserRole.ADMIN && !project.hasMember(user.getId())))
            throw new AccessDeniedException("You do not have access to this project.");
        return project;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> context(String email, UUID projectId) {
        Project project = authorizedProject(email, projectId);
        List<Task> projectTasks = tasks.findByProjectId(projectId);
        List<Meeting> projectMeetings = meetings.findByProjectIdOrderByScheduledAtDesc(projectId);
        List<Document> projectDocuments = documents.findByProjectIdAndArchivedFalse(projectId);
        User user = actor(email);
        boolean admin = user.getRole() == UserRole.ADMIN;
        List<Map<String, Object>> people = project.getMembers().stream()
                .filter(member -> member.getMembershipStatus() == MembershipStatus.ACTIVE
                        && member.getCompany() != null && member.getCompany().getId().equals(user.getCompany().getId()))
                .map(member -> Map.<String, Object>of("id", member.getId(), "name", member.getName()))
                .sorted(Comparator.comparing(person -> (String) person.get("name"), String.CASE_INSENSITIVE_ORDER)).toList();
        List<Map<String, Object>> taskViews = projectTasks.stream().map(task -> {
            Map<String, Object> item = new LinkedHashMap<>(); item.put("id", task.getId()); item.put("title", task.getTitle());
            item.put("status", task.getStatus()); item.put("assignee", task.getAssignee() == null ? null : task.getAssignee().getName());
            item.put("dueDate", task.getDueDate()); return item;
        }).toList();
        List<Meeting> visibleMeetings = projectMeetings.stream().filter(meeting -> admin
                || meeting.getOrganizer().getId().equals(user.getId()) || project.hasMember(user.getId())
                || meeting.getInvitees().stream().anyMatch(invitee -> invitee.getId().equals(user.getId()))).toList();
        Map<UUID, List<MeetingActionItem>> meetingActions = visibleMeetings.isEmpty() ? Map.of() : actionItems.findByMeetingIdInOrderByCreatedAtAsc(
                visibleMeetings.stream().map(Meeting::getId).toList()).stream().collect(Collectors.groupingBy(item -> item.getMeeting().getId()));
        List<Map<String, Object>> meetingViews = visibleMeetings.stream().map(meeting -> {
            Map<String, Object> item = new LinkedHashMap<>(); item.put("id", meeting.getId()); item.put("title", meeting.getTitle());
            item.put("scheduledAt", meeting.getScheduledAt()); item.put("summary", meeting.getSummary());
            item.put("actionItems", meetingActions.getOrDefault(meeting.getId(), List.of()).stream()
                    .map(action -> Map.of("id", action.getId(), "title", action.getTitle(), "status", action.getStatus())).toList());
            return item;
        }).toList();
        List<Map<String, Object>> documentViews = projectDocuments.stream().filter(document -> canRead(user, document))
                .map(document -> Map.<String, Object>of("id", document.getId(), "title", document.getTitle(), "owner", document.getOwner().getName(), "createdAt", document.getCreatedAt())).toList();
        Set<UUID> taskIds = projectTasks.stream().map(Task::getId).collect(Collectors.toSet());
        Set<UUID> meetingIds = visibleMeetings.stream().map(Meeting::getId).collect(Collectors.toSet());
        Set<UUID> documentIds = projectDocuments.stream().filter(document -> canRead(user, document)).map(Document::getId).collect(Collectors.toSet());
        List<ActivityLog> recent = new ArrayList<>();
        recent.addAll(activities.findTop20ByEntityTypeAndEntityIdInOrderByCreatedAtDesc("PROJECT", Set.of(projectId)));
        Set<UUID> actionIds = meetingActions.values().stream().flatMap(Collection::stream).map(MeetingActionItem::getId).collect(Collectors.toSet());
        addActivities(recent, "TASK", taskIds); addActivities(recent, "MEETING", meetingIds);
        addActivities(recent, "MEETING_ACTION", actionIds); addActivities(recent, "DOCUMENT", documentIds);
        List<Map<String, Object>> activityViews = recent.stream().distinct().sorted(Comparator.comparing(ActivityLog::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12).map(item -> Map.<String, Object>of("id", item.getId(), "summary", item.getSummary(),
                        "actor", item.getActor() == null ? "System" : item.getActor().getName(), "createdAt", item.getCreatedAt())).toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("project", Map.of("id", project.getId(), "name", project.getName(), "status", project.getStatus()));
        result.put("people", people); result.put("tasks", taskViews); result.put("meetings", meetingViews);
        result.put("documents", documentViews); result.put("activity", activityViews);
        return result;
    }

    private void addActivities(List<ActivityLog> target, String type, Set<UUID> ids) {
        if (!ids.isEmpty()) target.addAll(activities.findTop20ByEntityTypeAndEntityIdInOrderByCreatedAtDesc(type, ids));
    }

    private boolean canRead(User user, Document document) {
        return !document.isArchived() && document.getOwner().getCompany() != null
                && document.getOwner().getCompany().getId().equals(user.getCompany().getId())
                && (user.getRole() == UserRole.ADMIN || "COMPANY".equals(document.getVisibility())
                    || document.getOwner().getId().equals(user.getId()) || document.getProject().hasMember(user.getId()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> understand(String email, UUID projectId, String question) {
        Project project = authorizedProject(email, projectId);
        if (question == null || question.isBlank()) question = "Understand this project";
        if (question.length() > 1000) throw new IllegalArgumentException("Questions must be 1000 characters or fewer.");
        Map<String, Object> context = context(email, projectId);
        return insights.answerProject(email, project, context, question);
    }
}
