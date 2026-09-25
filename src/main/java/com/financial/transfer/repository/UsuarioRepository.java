package com.financial.transfer.repository;

import com.financial.transfer.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCpf(String cpf);
}
