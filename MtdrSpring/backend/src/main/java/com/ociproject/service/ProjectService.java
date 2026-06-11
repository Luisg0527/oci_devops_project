package com.ociproject.service;

import com.ociproject.model.*;
import com.ociproject.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectSprintRepository projectSprintRepository;

    public List<Project> findAll() {
        return projectRepository.findAllByDeletedFalse();
    }

    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }

    public List<Project> findByManager(Long managerId) {
        return projectRepository.findByManagerUserIdAndDeletedFalse(managerId);
    }

    /** Proyectos donde el usuario figura en PROJECT_MEMBERS. */
    public List<Project> findByMemberUserId(Long userId) {
        return memberRepository.findByIdUserIdAndDeletedFalse(userId).stream()
                .map(ProjectMember::getProject)
                .filter(p -> p != null && !Boolean.TRUE.equals(p.getDeleted()))
                .distinct()
                .collect(Collectors.toList());
    }

    public Optional<String> findMemberRoleInProject(Long projectId, Long userId) {
        return memberRepository.findByIdUserIdAndDeletedFalse(userId).stream()
                .filter(m -> projectId.equals(m.getId().getProjectId()))
                .map(ProjectMember::getRoleInProject)
                .filter(r -> r != null && !r.isBlank())
                .findFirst();
    }

    public List<Project> findByStatus(Project.Status status) {
        return projectRepository.findByStatusAndDeletedFalse(status);
    }

    public List<ProjectMember> findMembers(Long projectId) {
        return memberRepository.findByIdProjectIdAndDeletedFalse(projectId);
    }

    public List<ProjectSprint> findSprints(Long projectId) {
        return projectSprintRepository.findByIdProjectId(projectId);
    }

    @Transactional
    public Project save(Project project) {
        return projectRepository.save(project);
    }

    @Transactional
    public Project updateStatus(Long id, Project.Status status) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
        project.setStatus(status);
        return projectRepository.save(project);
    }

    @Transactional
    public ProjectMember addMember(Long projectId, User user, String roleInProject) {
        if (memberRepository.existsByIdProjectIdAndIdUserIdAndDeletedFalse(projectId, user.getUserId())) {
            throw new IllegalStateException("User " + user.getUserId() + " is already a member of project " + projectId);
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));
        ProjectMemberId memberId = new ProjectMemberId(projectId, user.getUserId());
        ProjectMember member = ProjectMember.builder()
                .id(memberId)
                .project(project)
                .user(user)
                .roleInProject(roleInProject)
                .build();
        return memberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        ProjectMemberId memberId = new ProjectMemberId(projectId, userId);
        ProjectMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Member not found in project " + projectId));
        member.setDeleted(true);
        member.setDeletedAt(LocalDateTime.now());
        memberRepository.save(member);
    }

    @Transactional
    public void softDelete(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + id));
        project.setDeleted(true);
        project.setDeletedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    @Transactional
    public ProjectSprint assignSprint(Long projectId, Sprint sprint, boolean active) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        List<ProjectSprint> currentSprints = projectSprintRepository.findByIdProjectId(projectId);
        int nextSprintNumber = currentSprints.stream()
                .map(ProjectSprint::getSprintNumber)
                .filter(number -> number != null && number > 0)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        if (active) {
            currentSprints.stream()
                    .filter(ps -> Boolean.TRUE.equals(ps.getActive()))
                    .forEach(ps -> ps.setActive(false));
            projectSprintRepository.saveAll(currentSprints);
        }

        ProjectSprint projectSprint = ProjectSprint.builder()
                .id(new ProjectSprintId(projectId, sprint.getSprintId()))
                .project(project)
                .sprint(sprint)
                .sprintNumber(nextSprintNumber)
                .active(active)
                .build();
        return projectSprintRepository.save(projectSprint);
    }

    @Transactional
    public void syncSprintActiveForProject(Long projectId, Long sprintId, boolean active) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        List<ProjectSprint> currentSprints = projectSprintRepository.findByIdProjectId(projectId);
        ProjectSprint target = currentSprints.stream()
                .filter(ps -> ps.getSprint().getSprintId().equals(sprintId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "Sprint " + sprintId + " is not assigned to project " + projectId));

        if (active) {
            currentSprints.forEach(ps -> ps.setActive(ps.getSprint().getSprintId().equals(sprintId)));
        } else {
            target.setActive(false);
        }
        projectSprintRepository.saveAll(currentSprints);
    }
}
