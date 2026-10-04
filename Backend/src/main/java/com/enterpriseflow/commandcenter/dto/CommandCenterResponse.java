package com.enterpriseflow.commandcenter.dto;

import java.time.LocalDate;
import java.util.List;

public record CommandCenterResponse(
        Summary summary,
        List<AttentionItem> attentionItems,
        String companyHealth,
        String healthExplanation,
        List<ProjectHealth> projectHealth,
        List<EmployeeWorkload> workload,
        String aiInsight
) {
    public record Summary(long activeProjects, long openTasks, long overdueTasks,
                          long upcomingDeadlines, long completedTasks, long activeEmployees) { }

    public record AttentionItem(String id, String title, String description, String href, String severity) { }

    public record ProjectHealth(String id, String name, String status, long openTasks,
                                long completedTasks, long overdueTasks, LocalDate dueDate,
                                int completionPercent, String health, String href) { }

    public record EmployeeWorkload(String userId, String name, long openTasks,
                                   long completedTasks, long overdueTasks) { }
}
