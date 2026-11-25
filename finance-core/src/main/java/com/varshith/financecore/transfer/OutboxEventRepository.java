package com.varshith.financecore.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {}