package teste.delta.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import teste.delta.model.Agendamento;

public interface AgendamentoRepository extends JpaRepository<Agendamento, UUID> {}