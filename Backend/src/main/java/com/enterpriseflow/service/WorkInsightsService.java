package com.enterpriseflow.service;

import com.enterpriseflow.entity.*;
import com.enterpriseflow.repository.ProjectRepository;
import com.enterpriseflow.repository.MeetingRepository;
import com.enterpriseflow.repository.TaskRepository;
import com.enterpriseflow.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Builds work insights only from records the authenticated user may access. */
@Service
public class WorkInsightsService {
    private enum WorkIntent { PENDING_TASKS, OVERDUE_TASKS, EMPLOYEE_TASK_BREAKDOWN, UPCOMING_DEADLINES, MEETINGS, DELAYED_PROJECTS, PROJECT_STATUS, COMPLETED_TASKS, WORK_SUMMARY }
    private static final int UPCOMING_DAYS = 7;
    private static final int MAX_SOURCES = 50;

    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final MeetingRepository meetings;

    public WorkInsightsService(UserRepository users, ProjectRepository projects, TaskRepository tasks, MeetingRepository meetings) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.meetings = meetings;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> answer(String email, String question, List<Map<String, Object>> authorizedMeetings) {
        WorkIntent intent = detectIntent(question);
        if (intent == null) return Map.of("answer", "I can't determine an answer from the supported work insights. Try asking about pending tasks, deadlines, meetings, overdue tasks, delayed projects, project status, or a work summary.", "sources", List.of());
        User actor = users.findByEmail(email).orElseThrow();
        if (actor.getMembershipStatus() != MembershipStatus.ACTIVE || actor.getCompany() == null) {
            throw new AccessDeniedException("Your company access is not active.");
        }

        UUID companyId = actor.getCompany().getId();
        boolean admin = actor.getRole() == UserRole.ADMIN;
        List<Project> visibleProjects = admin
                ? projects.findActiveByCompanyId(companyId)
                : projects.findVisibleByCompanyIdAndUserId(companyId, actor.getId());
        Set<UUID> visibleProjectIds = visibleProjects.stream().map(Project::getId).collect(Collectors.toSet());
        List<Task> visibleTasks = (admin
                ? tasks.findByProjectCompanyId(companyId)
                : tasks.findByAssigneeIdAndProjectCompanyId(actor.getId(), companyId))
                .stream()
                .filter(task -> visibleProjectIds.contains(task.getProject().getId()))
                .toList();

        boolean personalQuestion = hasPersonalReference(question);
        List<Task> personalTasks = personalQuestion || !admin
                ? tasks.findByAssigneeIdAndProjectCompanyId(actor.getId(), companyId).stream()
                    .filter(task -> visibleProjectIds.contains(task.getProject().getId())).toList()
                : visibleTasks;
        List<Map<String, Object>> meetingScope = admin
                ? personalMeetings(actor, companyId)
                : authorizedMeetings;

        LocalDate today = LocalDate.now();
        LocalDate through = today.plusDays(UPCOMING_DAYS);
        Predicate<Task> incomplete = task -> !"DONE".equalsIgnoreCase(task.getStatus());
        List<Project> delayedProjects = visibleProjects.stream()
                .filter(project -> !"COMPLETED".equalsIgnoreCase(project.getStatus()))
                .filter(project -> project.getDueDate() != null && project.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(Project::getDueDate))
                .toList();
        List<Task> pendingTasks = personalTasks.stream().filter(task -> !"DONE".equalsIgnoreCase(task.getStatus())).toList();
        List<Project> personalProjects = personalQuestion
                ? projects.findVisibleByCompanyIdAndUserId(companyId, actor.getId()) : visibleProjects;
        StringBuilder answer = new StringBuilder();
        List<Map<String, Object>> sources = new ArrayList<>();
        switch (intent) {
            case PENDING_TASKS -> appendList(answer, "Your pending tasks", pendingTasks, sources);
            case OVERDUE_TASKS -> {
                boolean companyQuestion = admin && !personalQuestion;
                if (companyQuestion && question.toLowerCase(Locale.ROOT).matches(".*\\b(who|employee|employees|team|by)\\b.*")) {
                    appendOverdueByEmployee(answer, companyId, visibleTasks, today, sources);
                } else {
                    List<Task> overdue = (companyQuestion ? visibleTasks : personalTasks).stream().filter(incomplete)
                            .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today)).toList();
                    if (!admin && question.toLowerCase(Locale.ROOT).matches(".*\\b(who|employee|employees|team|by)\\b.*")) {
                        answer.append("Company-wide employee assignments cannot be determined from your access. ");
                    }
                    appendList(answer, companyQuestion ? "Overdue company tasks" : "Your overdue tasks", overdue, sources);
                }
            }
            case EMPLOYEE_TASK_BREAKDOWN -> {
                String q = question.toLowerCase(Locale.ROOT);
                String category = q.matches(".*\\b(overdue|late|past due)\\b.*") ? "overdue"
                        : q.matches(".*\\b(completed|complete|finished)\\b.*") ? "completed" : "pending";
                if (admin && !personalQuestion) appendEmployeeTaskBreakdown(answer, companyId, visibleTasks, today, category, sources);
                else {
                    List<Task> own = category.equals("completed")
                            ? personalTasks.stream().filter(task -> "DONE".equalsIgnoreCase(task.getStatus())).toList()
                            : personalTasks.stream().filter(task -> !"DONE".equalsIgnoreCase(task.getStatus()))
                                .filter(task -> !category.equals("overdue") || (task.getDueDate() != null && task.getDueDate().isBefore(today))).toList();
                    answer.append("Company-wide employee breakdowns cannot be determined from your access. ");
                    appendList(answer, "Your " + category + " tasks", own, sources);
                }
            }
            case UPCOMING_DEADLINES -> {
                List<Task> dueTasks = personalTasks.stream().filter(incomplete)
                        .filter(task -> task.getDueDate() != null && !task.getDueDate().isBefore(today) && !task.getDueDate().isAfter(through))
                        .sorted(Comparator.comparing(Task::getDueDate)).toList();
                appendList(answer, "Your task deadlines in the next " + UPCOMING_DAYS + " days", dueTasks, sources);
                List<Project> dueProjects = personalProjects.stream().filter(project -> !"COMPLETED".equalsIgnoreCase(project.getStatus()))
                        .filter(project -> project.getDueDate() != null && !project.getDueDate().isBefore(today) && !project.getDueDate().isAfter(through))
                        .sorted(Comparator.comparing(Project::getDueDate)).toList();
                appendProjects(answer, "Deadlines for your accessible projects", dueProjects, sources);
            }
            case MEETINGS -> appendMeetings(answer, meetingScope, sources);
            case DELAYED_PROJECTS -> appendProjects(answer, "Delayed projects", delayedProjects, sources);
            case PROJECT_STATUS -> appendProjectStatus(answer, personalProjects, sources);
            case COMPLETED_TASKS -> appendList(answer, admin && !personalQuestion ? "Completed company tasks" : "Your completed tasks",
                    (admin && !personalQuestion ? visibleTasks : personalTasks).stream().filter(task -> "DONE".equalsIgnoreCase(task.getStatus())).toList(), sources);
            case WORK_SUMMARY -> {
                List<Task> summaryTasks = personalQuestion || !admin ? personalTasks : visibleTasks;
                List<Task> summaryOverdue = summaryTasks.stream().filter(incomplete)
                        .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today)).toList();
                List<Task> summaryUpcoming = summaryTasks.stream().filter(incomplete)
                        .filter(task -> task.getDueDate() != null && !task.getDueDate().isBefore(today) && !task.getDueDate().isAfter(through)).toList();
                appendSummary(answer, admin && !personalQuestion, actor, personalQuestion ? personalProjects : visibleProjects, summaryTasks, summaryOverdue, summaryUpcoming, delayedProjects);
                if (admin && !personalQuestion) appendEmployeeBreakdown(answer, companyId, visibleTasks, today);
                appendMeetings(answer, meetingScope, sources);
            }
        }
        if (sources.isEmpty()) {
            answer.append("\nNo matching work records were found in your authorized scope.");
        }
        return Map.of("answer", answer.toString(), "sources", sources.stream().limit(MAX_SOURCES).toList());
    }

    public static boolean isWorkQuestion(String question) {
        return detectIntent(question) != null;
    }

    /** Answers a project-scoped question using the same authenticated Work Insights entry point. */
    @Transactional(readOnly = true)
    public Map<String, Object> answerProject(String email, Project project, Map<String, Object> context, String question) {
        User actor = users.findByEmail(email).orElseThrow();
        if (actor.getMembershipStatus() != MembershipStatus.ACTIVE || actor.getCompany() == null
                || project.getCompany() == null || !actor.getCompany().getId().equals(project.getCompany().getId())
                || (actor.getRole() != UserRole.ADMIN && !project.hasMember(actor.getId())))
            throw new AccessDeniedException("You do not have access to this project.");
        @SuppressWarnings("unchecked") List<Map<String, Object>> people = (List<Map<String, Object>>) context.getOrDefault("people", List.of());
        @SuppressWarnings("unchecked") List<Map<String, Object>> projectTasks = (List<Map<String, Object>>) context.getOrDefault("tasks", List.of());
        @SuppressWarnings("unchecked") List<Map<String, Object>> projectMeetings = (List<Map<String, Object>>) context.getOrDefault("meetings", List.of());
        @SuppressWarnings("unchecked") List<Map<String, Object>> documents = (List<Map<String, Object>>) context.getOrDefault("documents", List.of());
        @SuppressWarnings("unchecked") List<Map<String, Object>> activity = (List<Map<String, Object>>) context.getOrDefault("activity", List.of());
        String q = question.toLowerCase(Locale.ROOT);
        String answer;
        List<Map<String, Object>> sources = new ArrayList<>();
        if (q.contains("who") || q.contains("people") || q.contains("working")) {
            answer = people.isEmpty() ? "No additional project context is available." : "People on " + project.getName() + ": "
                    + people.stream().map(person -> Objects.toString(person.get("name"))).collect(Collectors.joining(", ")) + ".";
        } else if (q.contains("overdue") || q.contains("past due")) {
            LocalDate today = LocalDate.now();
            List<Map<String, Object>> overdue = projectTasks.stream().filter(task -> task.get("dueDate") instanceof LocalDate due
                    && due.isBefore(today) && !"DONE".equalsIgnoreCase(Objects.toString(task.get("status"), ""))).toList();
            answer = overdue.isEmpty() ? "There are no overdue tasks recorded for " + project.getName() + "."
                    : "Overdue tasks in " + project.getName() + ": " + overdue.stream().map(task -> Objects.toString(task.get("title"))).collect(Collectors.joining(", ")) + ".";
            sources.addAll(overdue);
        } else if (q.contains("meeting")) {
            answer = projectMeetings.isEmpty() ? "No additional project context is available." : "Meetings related to " + project.getName() + ": "
                    + projectMeetings.stream().map(meeting -> Objects.toString(meeting.get("title"))).collect(Collectors.joining(", ")) + ".";
            sources.addAll(projectMeetings);
        } else if (q.contains("document")) {
            answer = documents.isEmpty() ? "No additional project context is available." : "Documents associated with " + project.getName() + ": "
                    + documents.stream().map(document -> Objects.toString(document.get("title"))).collect(Collectors.joining(", ")) + ".";
            sources.addAll(documents);
        } else if (q.contains("recent") || q.contains("changed") || q.contains("happened")) {
            answer = activity.isEmpty() ? "No additional project context is available." : "Recent recorded activity in " + project.getName() + ": "
                    + activity.stream().limit(5).map(item -> Objects.toString(item.get("summary"))).collect(Collectors.joining("; ")) + ".";
            sources.addAll(activity);
        } else {
            if (people.isEmpty() && projectTasks.isEmpty() && projectMeetings.isEmpty() && documents.isEmpty() && activity.isEmpty())
                return Map.of("answer", "No additional project context is available.", "sources", List.of());
            long open = projectTasks.stream().filter(task -> !"DONE".equalsIgnoreCase(Objects.toString(task.get("status"), ""))).count();
            long overdue = projectTasks.stream().filter(task -> task.get("dueDate") instanceof LocalDate due && due.isBefore(LocalDate.now())
                    && !"DONE".equalsIgnoreCase(Objects.toString(task.get("status"), ""))).count();
            long actions = projectMeetings.stream().flatMap(meeting -> ((List<?>) meeting.getOrDefault("actionItems", List.of())).stream()).count();
            answer = project.getName() + " is " + project.getStatus().toLowerCase(Locale.ROOT).replace('_', ' ') + " with " + open
                    + " open tasks across " + people.size() + " people, " + overdue + " overdue tasks, " + projectMeetings.size()
                    + " related meetings, " + actions + " meeting action items, and " + documents.size() + " associated documents.";
            sources.addAll(projectTasks.stream().limit(10).toList()); sources.addAll(projectMeetings.stream().limit(5).toList());
            sources.addAll(documents.stream().limit(5).toList());
        }
        List<Map<String, Object>> sourceViews = sources.stream().limit(MAX_SOURCES).map(item -> {
            Map<String, Object> source = new LinkedHashMap<>(); source.put("id", Objects.toString(item.get("id"), ""));
            source.put("title", Objects.toString(item.get("title"), Objects.toString(item.get("summary"), "Project record")));
            source.put("excerpt", Objects.toString(item.get("summary"), Objects.toString(item.get("status"), "Authorized project record")));
            return source;
        }).toList();
        return Map.of("answer", answer, "sources", sourceViews);
    }

    private static WorkIntent detectIntent(String question) {
        if (question == null || question.isBlank()) return null;
        String q = question.toLowerCase(Locale.ROOT);
        if (q.matches(".*\\b(meeting|meetings|calendar)\\b.*")) return WorkIntent.MEETINGS;
        if (q.matches(".*\\b(delayed|delay|behind schedule|past due|overdue)\\b.*") && q.matches(".*\\b(project|projects)\\b.*")) return WorkIntent.DELAYED_PROJECTS;
        if (q.matches(".*\\b(completed|complete|finished|pending|overdue|late)\\b.*")
                && q.matches(".*\\b(employee|employees|team|by)\\b.*")) return WorkIntent.EMPLOYEE_TASK_BREAKDOWN;
        if (q.matches(".*\\b(overdue|late|past due)\\b.*")) return WorkIntent.OVERDUE_TASKS;
        if (q.matches(".*\\b(deadline|deadlines|due|upcoming)\\b.*")) return WorkIntent.UPCOMING_DEADLINES;
        if (q.matches(".*\\b(pending|incomplete|unfinished)\\b.*")) return WorkIntent.PENDING_TASKS;
        if (q.matches(".*\\b(completed|complete|finished)\\b.*") && q.matches(".*\\b(task|tasks|work)\\b.*")) return WorkIntent.COMPLETED_TASKS;
        if (q.matches(".*\\b(project|projects)\\b.*") && q.matches(".*\\b(status|progress)\\b.*")) return WorkIntent.PROJECT_STATUS;
        if (q.matches(".*\\b(summary|summarize|overview|workload|company-wide|overall)\\b.*")) return WorkIntent.WORK_SUMMARY;
        return null;
    }

    private static boolean hasPersonalReference(String question) {
        String q = question.toLowerCase(Locale.ROOT);
        return q.matches(".*\\b(my|mine)\\s+(work|tasks?|deadlines?|projects?|meetings?|overdue|pending)\\b.*")
                || q.matches(".*\\b(for|to) me\\b.*") || q.matches(".*\\bi have\\b.*") || q.matches(".*\\bmine\\b.*");
    }

    private List<Map<String, Object>> personalMeetings(User actor, UUID companyId) {
        return meetings.findAllByOrderByScheduledAtDesc().stream()
                .filter(meeting -> meeting.getOrganizer().getCompany() != null && meeting.getOrganizer().getCompany().getId().equals(companyId))
                .filter(meeting -> meeting.getOrganizer().getId().equals(actor.getId())
                        || meeting.getInvitees().stream().anyMatch(invitee -> invitee.getId().equals(actor.getId()))
                        || (meeting.getProject() != null && meeting.getProject().hasMember(actor.getId())))
                .map(meeting -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("id", meeting.getId()); data.put("title", meeting.getTitle());
                    data.put("scheduledAt", meeting.getScheduledAt()); data.put("summary", meeting.getSummary());
                    return data;
                }).toList();
    }

    private void appendOverdueByEmployee(StringBuilder out, UUID companyId, List<Task> tasks, LocalDate today,
                                         List<Map<String, Object>> sources) {
        appendEmployeeTaskBreakdown(out, companyId, tasks, today, "overdue", sources);
    }

    private void appendEmployeeTaskBreakdown(StringBuilder out, UUID companyId, List<Task> tasks, LocalDate today,
                                             String category, List<Map<String, Object>> sources) {
        out.append(category.substring(0, 1).toUpperCase(Locale.ROOT)).append(category.substring(1))
                .append(" tasks by active employee:");
        List<User> members = users.findByCompanyIdAndMembershipStatusOrderByCreatedAtDesc(companyId, MembershipStatus.ACTIVE);
        boolean any = false;
        for (User member : members) {
            List<Task> matching = tasks.stream().filter(task -> task.getAssignee() != null
                    && task.getAssignee().getId().equals(member.getId()))
                    .filter(task -> switch (category) {
                        case "completed" -> "DONE".equalsIgnoreCase(task.getStatus());
                        case "overdue" -> !"DONE".equalsIgnoreCase(task.getStatus()) && task.getDueDate() != null && task.getDueDate().isBefore(today);
                        default -> !"DONE".equalsIgnoreCase(task.getStatus());
                    }).toList();
            if (!matching.isEmpty()) {
                any = true;
                out.append("\n").append(member.getName()).append(": ").append(matching.size()).append(' ').append(category).append(" task(s).");
                for (Task task : matching) {
                    String line = task.getTitle() + " — " + task.getProject().getName() + ", due " + task.getDueDate();
                    out.append("\n• ").append(line);
                    addSource(sources, task.getId(), task.getTitle(), line);
                }
            }
        }
        if (!any) out.append(" none recorded.");
    }

    private void appendSummary(StringBuilder out, boolean admin, User actor, List<Project> projects, List<Task> tasks,
                               List<Task> overdue, List<Task> upcoming, List<Project> delayed) {
        long completed = tasks.stream().filter(task -> "DONE".equalsIgnoreCase(task.getStatus())).count();
        long pending = tasks.size() - completed;
        out.append(admin ? "Company work summary" : "Your work summary").append(" (based on stored records): ")
                .append(projects.size()).append(" active projects, ").append(tasks.size()).append(" in-scope tasks, ")
                .append(completed).append(" completed, ").append(pending).append(" pending, ")
                .append(overdue.size()).append(" overdue tasks, and ").append(upcoming.size())
                .append(" task deadlines in the next ").append(UPCOMING_DAYS).append(" days.");
        if (admin) out.append(" There are ").append(delayed.size()).append(" delayed projects.");
        out.append("\nScope: ").append(admin ? actor.getCompany().getName() + " company" : "your assigned tasks, accessible projects, and authorized meetings").append('.');
    }

    private void appendEmployeeBreakdown(StringBuilder out, UUID companyId, List<Task> tasks, LocalDate today) {
        out.append("\n\nTask counts by active employee:");
        List<User> members = users.findByCompanyIdAndMembershipStatusOrderByCreatedAtDesc(companyId, MembershipStatus.ACTIVE);
        for (User member : members) {
            List<Task> assigned = tasks.stream().filter(task -> task.getAssignee() != null && task.getAssignee().getId().equals(member.getId())).toList();
            long completed = assigned.stream().filter(task -> "DONE".equalsIgnoreCase(task.getStatus())).count();
            long overdue = assigned.stream().filter(task -> !"DONE".equalsIgnoreCase(task.getStatus()) && task.getDueDate() != null && task.getDueDate().isBefore(today)).count();
            out.append("\n• ").append(member.getName()).append(" — ").append(completed).append(" completed, ")
                    .append(assigned.size() - completed).append(" pending, ").append(overdue).append(" overdue.");
        }
    }

    private void appendList(StringBuilder out, String title, List<Task> items, List<Map<String, Object>> sources) {
        out.append("\n\n").append(title).append(':');
        if (items.isEmpty()) { out.append(" none recorded."); return; }
        for (Task task : items) {
            String line = task.getTitle() + " — " + task.getProject().getName() + ", due " + task.getDueDate() + ", " + task.getStatus();
            if (task.getAssignee() != null) line += ", assigned to " + task.getAssignee().getName();
            out.append("\n• ").append(line).append('.');
            addSource(sources, task.getId(), task.getTitle(), line);
        }
    }

    private void appendProjects(StringBuilder out, String title, List<Project> items, List<Map<String, Object>> sources) {
        out.append("\n\n").append(title).append(':');
        if (items.isEmpty()) { out.append(" none recorded."); return; }
        for (Project project : items) {
            String line = project.getName() + " — " + project.getStatus() + ", due " + project.getDueDate();
            out.append("\n• ").append(line).append('.');
            addSource(sources, project.getId(), project.getName(), line);
        }
    }

    private void appendProjectStatus(StringBuilder out, List<Project> items, List<Map<String, Object>> sources) {
        out.append("\n\nProject status:");
        if (items.isEmpty()) { out.append(" no accessible active projects recorded."); return; }
        for (Project project : items) {
            String line = project.getName() + " — " + project.getStatus();
            if (project.getDueDate() != null) line += ", due " + project.getDueDate();
            out.append("\n• ").append(line).append('.');
            addSource(sources, project.getId(), project.getName(), line);
        }
    }

    private void appendMeetings(StringBuilder out, List<Map<String, Object>> meetings, List<Map<String, Object>> sources) {
        java.time.Instant now = java.time.Instant.now();
        List<Map<String, Object>> upcoming = meetings.stream()
                .filter(meeting -> meeting.get("scheduledAt") instanceof java.time.Instant)
                .filter(meeting -> ((java.time.Instant) meeting.get("scheduledAt")).isAfter(now))
                .sorted(Comparator.comparing(meeting -> (java.time.Instant) meeting.get("scheduledAt"))).toList();
        out.append("Upcoming authorized meetings:");
        if (upcoming.isEmpty()) out.append(" none recorded.");
        for (Map<String, Object> meeting : upcoming) {
            String title = Objects.toString(meeting.get("title"), "Meeting");
            Object scheduledAt = meeting.get("scheduledAt");
            String summary = Objects.toString(meeting.get("summary"), "");
            String line = title + (scheduledAt == null ? "" : " — scheduled " + scheduledAt) + (summary.isBlank() ? "" : "; " + summary);
            out.append("\n• ").append(line);
            addSource(sources, meeting.get("id"), title, line);
        }
        meetings.stream().filter(meeting -> meeting.get("summary") != null && !Objects.toString(meeting.get("summary"), "").isBlank())
                .filter(meeting -> !upcoming.contains(meeting)).limit(5).forEach(meeting -> {
                    String title = Objects.toString(meeting.get("title"), "Meeting");
                    String line = title + " — " + Objects.toString(meeting.get("summary"), "");
                    out.append("\nRecent authorized summary: ").append(line);
                    addSource(sources, meeting.get("id"), title, line);
                });
    }

    private void addSource(List<Map<String, Object>> sources, Object id, String title, String excerpt) {
        if (id != null && sources.size() < MAX_SOURCES && sources.stream().noneMatch(source -> id.toString().equals(source.get("id")))) {
            sources.add(Map.of("id", id.toString(), "title", title, "excerpt", excerpt));
        }
    }
}
