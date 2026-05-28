package com.meetingsum.repository;

import com.meetingsum.model.entity.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, String> {

    Page<Meeting> findByStatus(String status, Pageable pageable);

    Page<Meeting> findByTitleContainingIgnoreCase(String search, Pageable pageable);

    Page<Meeting> findByStatusAndTitleContainingIgnoreCase(String status, String search, Pageable pageable);
}
