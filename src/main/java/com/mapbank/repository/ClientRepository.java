package com.mapbank.repository;

import com.mapbank.domain.enums.ClientStatus;
import com.mapbank.domain.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para gerenciamento da persistência de clientes titulares.
 *
 * @author Matheus Araújo Pereira
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByDocument(String document);

    Optional<Client> findByEmail(String email);

    boolean existsByDocument(String document);

    boolean existsByEmail(String email);

    Page<Client> findAllByStatus(ClientStatus status, Pageable pageable);
}
