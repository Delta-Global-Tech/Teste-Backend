package teste.delta.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import teste.delta.model.Cliente;

/**
 * ClienteRepository
 */
public interface ClienteRepository extends JpaRepository<Cliente, UUID>{

    
}