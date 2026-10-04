package com.enterpriseflow.repository;
import com.enterpriseflow.entity.MeetingActionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MeetingActionItemRepository extends JpaRepository<MeetingActionItem,UUID>{List<MeetingActionItem> findByMeetingIdOrderByCreatedAtAsc(UUID meetingId); List<MeetingActionItem> findByMeetingIdInOrderByCreatedAtAsc(java.util.Collection<UUID> meetingIds);}
