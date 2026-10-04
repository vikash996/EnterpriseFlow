package com.enterpriseflow.service;

import com.enterpriseflow.commandcenter.dto.CommandCenterResponse;
import com.enterpriseflow.commandcenter.dto.CommandCenterResponse.AttentionItem;
import com.enterpriseflow.commandcenter.dto.CommandCenterResponse.EmployeeWorkload;
import com.enterpriseflow.commandcenter.dto.CommandCenterResponse.ProjectHealth;
import com.enterpriseflow.commandcenter.dto.CommandCenterResponse.Summary;
import com.enterpriseflow.entity.MembershipStatus;
import com.enterpriseflow.entity.Project;
import com.enterpriseflow.entity.Task;
import com.enterpriseflow.entity.User;
import com.enterpriseflow.entity.UserRole;
import com.enterpriseflow.repository.ProjectRepository;
import com.enterpriseflow.repository.TaskRepository;
import com.enterpriseflow.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class CommandCenterService {
    private static final Logger log = LoggerFactory.getLogger(CommandCenterService.class);
    private static final int UPCOMING_DAYS = 7;
    private final UserRepository users;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final KnowledgeService knowledge;

    public CommandCenterService(UserRepository users, ProjectRepository projects,
                                TaskRepository tasks, KnowledgeService knowledge) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.knowledge = knowledge;
    }

    @Transactional(readOnly = true)
    public CommandCenterResponse get(String email) {
        String stage = "authenticated user lookup";
        try {
        stage = "authenticated user lookup";
        User actor = users.findByEmail(email).orElseThrow();
        stage = "active membership and administrator authorization";
        if (actor.getMembershipStatus() != MembershipStatus.ACTIVE || actor.getCompany() == null) {
            throw new AccessDeniedException("Your company access is not active.");
        }
        if (actor.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Administrator access is required.");
        }

        UUID companyId = actor.getCompany().getId();
        LocalDate today = LocalDate.now();
        LocalDate through = today.plusDays(UPCOMING_DAYS);
        stage = "company-scoped project query";
        List<Project> companyProjects = projects.findActiveByCompanyId(companyId);
        Set<UUID> projectIds = companyProjects.stream().map(Project::getId).collect(Collectors.toSet());
        stage = "company-scoped task query and project filtering";
        List<Task> companyTasks = tasks.findByProjectCompanyId(companyId).stream()
                .filter(task -> projectIds.contains(task.getProject().getId())).toList();
        stage = "company task and deadline aggregation";
        Predicate<Task> open = task -> !isCompleted(task);
        Predicate<Project> incompleteProject = project -> !"COMPLETED".equalsIgnoreCase(project.getStatus());

        long openTasks = companyTasks.stream().filter(open).count();
        long overdueTasks = companyTasks.stream().filter(open).filter(task -> dueBefore(task, today)).count();
        long completedTasks = companyTasks.stream().filter(CommandCenterService::isCompleted).count();
        long taskDeadlines = companyTasks.stream().filter(open).filter(task -> dueBetween(task, today, through)).count();
        long projectDeadlines = companyProjects.stream().filter(incompleteProject)
                .filter(project -> project.getDueDate() != null && !project.getDueDate().isBefore(today)
                        && !project.getDueDate().isAfter(through)).count();
        List<Project> delayedProjects = companyProjects.stream().filter(incompleteProject)
                .filter(project -> project.getDueDate() != null && project.getDueDate().isBefore(today)).toList();
        List<Task> overdue = companyTasks.stream().filter(open).filter(task -> dueBefore(task, today)).toList();

        List<AttentionItem> attention = new ArrayList<>();
        if (!overdue.isEmpty()) attention.add(new AttentionItem("overdue-tasks", overdue.size() + " overdue " + plural(overdue.size(), "task", "tasks"),
                "Unfinished tasks with a due date before today.", "/tasks", "CRITICAL"));
        if (!delayedProjects.isEmpty()) attention.add(new AttentionItem("delayed-projects", delayedProjects.size() + " delayed " + plural(delayedProjects.size(), "project", "projects"),
                "Projects past their due date and not marked completed.", "/projects", "CRITICAL"));
        if (taskDeadlines > 0) attention.add(new AttentionItem("task-deadlines", taskDeadlines + " task " + plural(taskDeadlines, "deadline", "deadlines") + " in the next 7 days",
                "Unfinished task deadlines from today through " + through + ".", "/tasks", "ATTENTION"));
        if (projectDeadlines > 0) attention.add(new AttentionItem("project-deadlines", projectDeadlines + " project " + plural(projectDeadlines, "deadline", "deadlines") + " in the next 7 days",
                "Non-completed project deadlines from today through " + through + ".", "/projects", "ATTENTION"));

        stage = "per-project health aggregation";
        List<ProjectHealth> projectHealth = companyProjects.stream().map(project -> {
            List<Task> projectTasks = companyTasks.stream().filter(task -> task.getProject().getId().equals(project.getId())).toList();
            long done = projectTasks.stream().filter(CommandCenterService::isCompleted).count();
            long projectOpen = projectTasks.size() - done;
            long projectOverdue = projectTasks.stream().filter(open).filter(task -> dueBefore(task, today)).count();
            boolean projectLate = incompleteProject.test(project) && project.getDueDate() != null && project.getDueDate().isBefore(today);
            boolean approaching = incompleteProject.test(project) && project.getDueDate() != null
                    && !project.getDueDate().isBefore(today) && !project.getDueDate().isAfter(through);
            String health = projectOverdue > 0 || projectLate ? "CRITICAL" : approaching ? "ATTENTION" : "ON_TRACK";
            int progress = projectTasks.isEmpty() ? 0 : (int) Math.round(done * 100.0 / projectTasks.size());
            return new ProjectHealth(project.getId().toString(), project.getName(), project.getStatus(), projectOpen,
                    done, projectOverdue, project.getDueDate(), progress, health, "/projects");
        }).sorted(Comparator.comparing(ProjectHealth::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ProjectHealth::name, String.CASE_INSENSITIVE_ORDER)).toList();

        stage = "company active employee query and workload aggregation";
        List<EmployeeWorkload> workload = users.findByCompanyIdAndMembershipStatusOrderByCreatedAtDesc(companyId, MembershipStatus.ACTIVE)
                .stream().map(member -> workload(member, companyTasks, today))
                .sorted(Comparator.comparing(EmployeeWorkload::name, String.CASE_INSENSITIVE_ORDER)).toList();

        stage = "company health explanation";
        String health = overdueTasks > 0 || !delayedProjects.isEmpty() ? "CRITICAL"
                : taskDeadlines + projectDeadlines > 0 ? "ATTENTION_NEEDED" : "ON_TRACK";
        String explanation = health.equals("CRITICAL")
                ? "Critical because " + overdueTasks + " tasks are overdue and " + delayedProjects.size() + " projects are past due."
                : health.equals("ATTENTION_NEEDED")
                    ? "Attention needed because " + (taskDeadlines + projectDeadlines) + " deadlines fall within the next 7 days."
                    : "On track: no overdue tasks, delayed projects, or deadlines in the next 7 days.";
        stage = "permission-aware KnowledgeService and WorkInsightsService call";
        String aiInsight = String.valueOf(knowledge.ask(email, "Give me a company work summary").get("answer"));
        Summary summary = new Summary(companyProjects.size(), openTasks, overdueTasks, taskDeadlines + projectDeadlines,
                completedTasks, workload.size());
        stage = "CommandCenterResponse DTO construction";
        return new CommandCenterResponse(summary, List.copyOf(attention), health, explanation,
                projectHealth, workload, aiInsight);
        } catch (RuntimeException exception) {
            log.error("Command Center request failed during {}", stage, exception);
            throw exception;
        }
    }

    private static EmployeeWorkload workload(User member, List<Task> tasks, LocalDate today) {
        List<Task> assigned = tasks.stream().filter(task -> task.getAssignee() != null
                && task.getAssignee().getId().equals(member.getId())).toList();
        long completed = assigned.stream().filter(CommandCenterService::isCompleted).count();
        long overdue = assigned.stream().filter(task -> !isCompleted(task)).filter(task -> dueBefore(task, today)).count();
        return new EmployeeWorkload(member.getId().toString(), member.getName(), assigned.size() - completed, completed, overdue);
    }

    private static boolean isCompleted(Task task) { return "DONE".equalsIgnoreCase(task.getStatus()); }
    private static boolean dueBefore(Task task, LocalDate date) { return task.getDueDate() != null && task.getDueDate().isBefore(date); }
    private static boolean dueBetween(Task task, LocalDate from, LocalDate through) {
        return task.getDueDate() != null && !task.getDueDate().isBefore(from) && !task.getDueDate().isAfter(through);
    }
    private static String plural(long count, String singular, String plural) { return count == 1 ? singular : plural; }
}
