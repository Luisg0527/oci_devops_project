package com.ociproject.service.ai.source;

import com.ociproject.model.ProjectDocEmbedding.SourceType;
import com.ociproject.model.User;
import com.ociproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserSourceReader implements SourceReader {

    private final UserRepository userRepository;

    @Override
    public SourceType type() { return SourceType.USER; }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunk> readAll() {
        List<User> users = userRepository.findAllByDeletedFalse();
        List<DocumentChunk> out = new ArrayList<>(users.size());
        for (User u : users) {
            StringBuilder sb = new StringBuilder();
            sb.append("Usuario: ").append(u.getFullName() == null ? "" : u.getFullName()).append('\n');
            sb.append("Email: ").append(u.getEmail() == null ? "" : u.getEmail()).append('\n');
            if (u.getRole() != null) {
                sb.append("Rol: ").append(u.getRole().getRoleName()).append('\n');
            }
            if (u.getTeam() != null) {
                sb.append("Equipo: ").append(u.getTeam().getName())
                  .append(" (id=").append(u.getTeam().getTeamId()).append(")\n");
            }
            sb.append("Estado: ").append(u.getStatus() != null ? u.getStatus().name() : "ACTIVE").append('\n');
            out.add(new DocumentChunk(SourceType.USER, u.getUserId(), sb.toString()));
        }
        return out;
    }
}
