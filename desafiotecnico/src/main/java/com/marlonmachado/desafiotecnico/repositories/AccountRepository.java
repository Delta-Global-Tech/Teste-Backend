package com.marlonmachado.desafiotecnico.repositories;

import com.marlonmachado.desafiotecnico.entities.Account;
import com.marlonmachado.desafiotecnico.entities.enums.AccountStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    long countByAccountStatus(AccountStatus accountStatus);

    @Query("SELECT COALESCE(SUM(a.balanceCents), 0) FROM Account a WHERE a.user IS NOT NULL")
    Long sumBalanceOfUserAccounts();

    @Query("""
            SELECT a.id
            FROM Account a
            WHERE a.number = :accountNumber
            """)
    Optional<Long> findIdByAccountNumber(
            @Param("accountNumber") String accountNumber
    );
}
