package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.AttendanceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceGroupRepository extends JpaRepository<AttendanceGroup, Long> {

    Optional<AttendanceGroup> findByGroupName(String groupName);
}
