package com.enterpriseflow.service;
import com.enterpriseflow.entity.*;
import com.enterpriseflow.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service public class NotificationService {
 private final NotificationRepository r; private final UserRepository users;
 public NotificationService(NotificationRepository r,UserRepository users){this.r=r;this.users=users;}
 @Transactional(readOnly=true) public List<Map<String,Object>> list(String email){return r.findByRecipientEmailOrderByCreatedAtDesc(email).stream().map(this::view).toList();}
 @Transactional(readOnly=true) public List<Map<String,Object>> companyOverview(String email){User actor=users.findByEmail(email).orElseThrow();if(actor.getRole()!=UserRole.ADMIN||actor.getMembershipStatus()!=MembershipStatus.ACTIVE||actor.getCompany()==null)throw new org.springframework.security.access.AccessDeniedException("Administrator access is required.");return r.findTop50ByOrderByCreatedAtDesc().stream().filter(n->n.getRecipient().getCompany()!=null&&n.getRecipient().getCompany().getId().equals(actor.getCompany().getId())).map(n->{Map<String,Object>v=new LinkedHashMap<>(view(n));v.put("recipient",n.getRecipient().getName());return v;}).toList();}
 @Transactional public void read(String email,UUID id){Notification n=r.findById(id).orElseThrow(()->new NoSuchElementException("Notification not found."));if(!n.getRecipient().getEmail().equals(email))throw new org.springframework.security.access.AccessDeniedException("You cannot update this notification.");n.markRead();}
 @Transactional public void markAllRead(String email){r.findByRecipientEmailOrderByCreatedAtDesc(email).stream().filter(n->!n.isRead()).forEach(Notification::markRead);}
 private Map<String,Object>view(Notification n){return Map.of("id",n.getId(),"title",n.getTitle(),"body",n.getBody(),"type",n.getType(),"read",n.isRead(),"createdAt",n.getCreatedAt());}
}
