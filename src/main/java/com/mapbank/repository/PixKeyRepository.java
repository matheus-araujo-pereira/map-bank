package com.mapbank.repository;

import com.mapbank.domain.model.PixKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para chaves PIX.
 *
 * @author Matheus Araujo Pereira
 */
@Repository
public interface PixKeyRepository extends JpaRepository<PixKey, Long> {

    Optional<PixKey> findByKeyValue(String keyValue);

    boolean existsByKeyValue(String keyValue);

    List<PixKey> findByAccountId(Long accountId);

    long countByAccountId(Long accountId);
}
