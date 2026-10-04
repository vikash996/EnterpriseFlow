package com.enterpriseflow.entity;

import jakarta.persistence.*; import java.time.*; import java.util.*;
@Entity @Table(name="projects") public class Project {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @Column(nullable=false,length=160) private String name; @Column(columnDefinition="TEXT") private String description; @Column(nullable=false,length=32) private String status="PLANNING"; private boolean archived;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="owner_id",nullable=false) private User owner; private LocalDate dueDate; private Instant createdAt; private Instant updatedAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id") private Company company;
 @ManyToMany @JoinTable(name="project_members",joinColumns=@JoinColumn(name="project_id"),inverseJoinColumns=@JoinColumn(name="user_id")) private Set<User> members=new HashSet<>();
 protected Project(){} public Project(String n,String d,User o,LocalDate due){name=n;description=d;owner=o;company=o.getCompany();dueDate=due;members.add(o);} @PrePersist void c(){createdAt=updatedAt=Instant.now();}@PreUpdate void u(){updatedAt=Instant.now();}
 public UUID getId(){return id;} public String getName(){return name;} public String getDescription(){return description;} public String getStatus(){return status;} public boolean isArchived(){return archived;} public User getOwner(){return owner;} public Company getCompany(){return company;} public LocalDate getDueDate(){return dueDate;} public Instant getCreatedAt(){return createdAt;} public Set<User> getMembers(){return members;} public void update(String n,String d,String s,LocalDate due){name=n;description=d;status=s;dueDate=due;} public void archive(){archived=true;} public boolean addMember(User u){return members.add(u);} public boolean hasMember(UUID id){return owner.getId().equals(id)||members.stream().anyMatch(u->u.getId().equals(id));}
}
