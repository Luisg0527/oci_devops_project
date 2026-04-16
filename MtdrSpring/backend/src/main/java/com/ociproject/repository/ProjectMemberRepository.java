package com.ociproject.repository;

import com.ociproject.model.ProjectMember;
import com.ociproject.model.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findByIdProjectIdAndDeletedFalse(Long projectId);
    List<ProjectMember> findByIdUserIdAndDeletedFalse(Long userId);
    boolean existsByIdProjectIdAndIdUserIdAndDeletedFalse(Long projectId, Long userId);
}
