package com.ociproject.service;

import com.ociproject.TestFixtures;
import com.ociproject.model.*;
import com.ociproject.repository.ProjectMemberRepository;
import com.ociproject.repository.ProjectRepository;
import com.ociproject.repository.ProjectSprintRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock ProjectMemberRepository memberRepository;
    @Mock ProjectSprintRepository projectSprintRepository;

    @InjectMocks
    ProjectService service;

    @Test
    void findAll_returnsNonDeletedProjects() {
        Project p = TestFixtures.project1();
        when(projectRepository.findAllByDeletedFalse()).thenReturn(List.of(p));

        assertThat(service.findAll()).containsExactly(p);
    }

    @Test
    void findById_found_returnsProject() {
        Project p = TestFixtures.project1();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(p));

        assertThat(service.findById(1L)).isPresent().contains(p);
    }

    @Test
    void findByManager_returnsList() {
        Project p = TestFixtures.project1();
        when(projectRepository.findByManagerUserIdAndDeletedFalse(1L)).thenReturn(List.of(p));

        assertThat(service.findByManager(1L)).containsExactly(p);
    }

    @Test
    void save_persistsProject() {
        Project p = TestFixtures.project1();
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.save(p)).isSameAs(p);
    }

    @Test
    void updateStatus_changesStatusAndSaves() {
        Project p = TestFixtures.project1();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Project result = service.updateStatus(1L, Project.Status.ON_HOLD);

        assertThat(result.getStatus()).isEqualTo(Project.Status.ON_HOLD);
    }

    @Test
    void updateStatus_notFound_throwsEntityNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.updateStatus(99L, Project.Status.COMPLETED));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void addMember_newMember_savesMembership() {
        Project p = TestFixtures.project1();
        User user = TestFixtures.user5();
        when(memberRepository.existsByIdProjectIdAndIdUserIdAndDeletedFalse(1L, 5L)).thenReturn(false);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProjectMember saved = service.addMember(1L, user, "DEVELOPER");

        assertThat(saved.getRoleInProject()).isEqualTo("DEVELOPER");
        verify(memberRepository).save(any(ProjectMember.class));
    }

    @Test
    void addMember_duplicateMembership_throwsIllegalStateAndNeverSaves() {
        User user = TestFixtures.user5();
        when(memberRepository.existsByIdProjectIdAndIdUserIdAndDeletedFalse(1L, 5L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.addMember(1L, user, "DEVELOPER"));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void removeMember_softDeletesMembership() {
        ProjectMemberId memberId = new ProjectMemberId(1L, 5L);
        ProjectMember member = ProjectMember.builder().id(memberId).deleted(false).build();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.removeMember(1L, 5L);

        verify(memberRepository).save(argThat(m ->
                Boolean.TRUE.equals(m.getDeleted()) && m.getDeletedAt() != null
        ));
    }

    @Test
    void removeMember_notFound_throwsEntityNotFoundException() {
        ProjectMemberId memberId = new ProjectMemberId(1L, 99L);
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.removeMember(1L, 99L));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void softDelete_setsDeletedFlagAndTimestamp() {
        Project p = TestFixtures.project1();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(p));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.softDelete(1L);

        verify(projectRepository).save(argThat(proj ->
                Boolean.TRUE.equals(proj.getDeleted()) && proj.getDeletedAt() != null
        ));
    }

    @Test
    void softDelete_notFound_throwsEntityNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.softDelete(99L));
        verify(projectRepository, never()).save(any());
    }
}

