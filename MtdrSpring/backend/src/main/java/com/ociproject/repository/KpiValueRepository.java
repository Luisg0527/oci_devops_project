package com.ociproject.repository;

import com.ociproject.model.KpiValue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KpiValueRepository extends JpaRepository<KpiValue, Long> {
    List<KpiValue> findByScopeType(KpiValue.ScopeType scopeType);
    List<KpiValue> findByUserUserId(Long userId);
    List<KpiValue> findByProjectProjectId(Long projectId);
    List<KpiValue> findBySprintSprintId(Long sprintId);
    List<KpiValue> findByKpiTypeKpiTypeIdAndUserUserId(Long kpiTypeId, Long userId);
    List<KpiValue> findByKpiTypeKpiTypeIdAndProjectProjectId(Long kpiTypeId, Long projectId);
    List<KpiValue> findByKpiTypeKpiTypeIdAndSprintSprintId(Long kpiTypeId, Long sprintId);
}
