package com.ray.atten.middle.module.repository;

import com.ray.atten.middle.module.model.CardTemplate;
import com.ray.atten.middle.module.model.OaEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardTemplateRepository extends JpaRepository<CardTemplate, Long> {

    List<CardTemplate> findByActiveTrue();

    CardTemplate findByNameContains(String name);

    Optional<CardTemplate> findByUuid(UUID uuid);

    List<OaEmployee> findAllByUuidIn(List<UUID> uuids);
}
