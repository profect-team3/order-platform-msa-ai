package app.ai.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import app.ai.model.entity.AiHistory;

@Repository
public interface AiHistoryRepository extends JpaRepository<AiHistory, UUID> {
}
