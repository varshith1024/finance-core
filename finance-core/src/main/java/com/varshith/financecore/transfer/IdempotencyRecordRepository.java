package com.varshith.financecore.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    /**
     * Returns 1 if we claimed the key, 0 if it already existed.
     * If another transaction is inserting the same key right now, Postgres makes us WAIT
     * until it commits; then we get 0 and read its finished record.
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_records (id, scope, idem_key, request_hash, status, created_at, updated_at)
            VALUES (gen_random_uuid(), :scope, :key, :hash, 'IN_PROGRESS', now(), now())
            ON CONFLICT (scope, idem_key) DO NOTHING
            """, nativeQuery = true)
    int tryInsert(@Param("scope") String scope, @Param("key") String key, @Param("hash") String hash);

    @Query("select r from IdempotencyRecord r where r.scope = :scope and r.idemKey = :key")
    Optional<IdempotencyRecord> findByScopeAndKey(@Param("scope") String scope, @Param("key") String key);
}