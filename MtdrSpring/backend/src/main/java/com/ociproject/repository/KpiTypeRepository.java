package com.ociproject.repository;

import com.ociproject.model.KpiType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface KpiTypeRepository extends JpaRepository<KpiType, Long> {
    Optional<KpiType> findByName(String name);
    List<KpiType> findByCategory(String category);
}
