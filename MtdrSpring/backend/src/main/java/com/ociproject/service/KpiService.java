package com.ociproject.service;

import com.ociproject.model.KpiType;
import com.ociproject.model.KpiValue;
import com.ociproject.repository.KpiTypeRepository;
import com.ociproject.repository.KpiValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiService {

    private final KpiTypeRepository kpiTypeRepository;
    private final KpiValueRepository kpiValueRepository;

    public List<KpiType> findAllTypes() {
        return kpiTypeRepository.findAll();
    }

    public Optional<KpiType> findTypeById(Long id) {
        return kpiTypeRepository.findById(id);
    }

    public List<KpiType> findTypesByCategory(String category) {
        return kpiTypeRepository.findByCategory(category);
    }

    public List<KpiValue> findValuesByUser(Long userId) {
        return kpiValueRepository.findByUserUserId(userId);
    }

    public List<KpiValue> findValuesByProject(Long projectId) {
        return kpiValueRepository.findByProjectProjectId(projectId);
    }

    public List<KpiValue> findValuesBySprint(Long sprintId) {
        return kpiValueRepository.findBySprintSprintId(sprintId);
    }

    public List<KpiValue> findValuesByScope(KpiValue.ScopeType scopeType) {
        return kpiValueRepository.findByScopeType(scopeType);
    }

    @Transactional
    public KpiType saveType(KpiType kpiType) {
        return kpiTypeRepository.save(kpiType);
    }

    @Transactional
    public KpiValue recordValue(KpiValue kpiValue) {
        validateScope(kpiValue);
        return kpiValueRepository.save(kpiValue);
    }

    private void validateScope(KpiValue kpiValue) {
        switch (kpiValue.getScopeType()) {
            case USER    -> { if (kpiValue.getUser() == null)    throw new IllegalArgumentException("USER scope requires a user"); }
            case PROJECT -> { if (kpiValue.getProject() == null) throw new IllegalArgumentException("PROJECT scope requires a project"); }
            case SPRINT  -> { if (kpiValue.getSprint() == null)  throw new IllegalArgumentException("SPRINT scope requires a sprint"); }
            case GLOBAL  -> { /* no FK required */ }
        }
    }
}
