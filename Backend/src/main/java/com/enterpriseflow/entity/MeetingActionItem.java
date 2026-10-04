package com.enterpriseflow.entity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="meeting_action_items") public class MeetingActionItem {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="meeting_id",nullable=false) private Meeting meeting;
 @Column(nullable=false,length=200) private String title;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignee_id") private User assignee;
 private LocalDate dueDate; @Column(nullable=false,length=24) private String status="OPEN";
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="task_id") private Task task;
 private Instant createdAt; protected MeetingActionItem(){}
 public MeetingActionItem(Meeting m,String title,User assignee,LocalDate dueDate){this.meeting=m;this.title=title;this.assignee=assignee;this.dueDate=dueDate;}
 @PrePersist void created(){createdAt=Instant.now();} public UUID getId(){return id;} public Meeting getMeeting(){return meeting;} public String getTitle(){return title;} public User getAssignee(){return assignee;} public LocalDate getDueDate(){return dueDate;} public String getStatus(){return status;} public Task getTask(){return task;} public void convert(Task t){task=t;status="CONVERTED";}
}
