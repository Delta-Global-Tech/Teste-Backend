package com.marlonmachado.desafiotecnico.entities;

import com.marlonmachado.desafiotecnico.entities.enums.MovementType;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "movements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_movements_account_sequence",
                        columnNames = {"account_id", "sequence_number"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_movements_account_sequence",
                        columnList = "account_id, sequence_number DESC"
                ),
                @Index(
                        name = "idx_movements_account_created_at",
                        columnList = "account_id, created_at"
                ),
                @Index(
                        name = "idx_movements_transfer",
                        columnList = "transfer_id"
                )
        }
)
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id")
    private Transfer transfer;

    @Column(name = "sequence_number", nullable = false)
    private long sequence;

    @Enumerated(EnumType.STRING)
    private MovementType type;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(name = "balance_after_cents", nullable = false)
    private long balanceAfterCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;


    public Movement() {
    }

    public Movement(Account account, Transfer transfer, long sequence, MovementType type, long amountCents, long balanceAfterCents, Instant createdAt) {
        this.account = account;
        this.transfer = transfer;
        this.sequence = sequence;
        this.type = type;
        this.amountCents = amountCents;
        this.balanceAfterCents = balanceAfterCents;
        this.createdAt = createdAt;
    }

    public static Movement credit(Account account, Transfer transfer, long sequence, long amountCents, long balanceAfterCents, Instant createdAt) {
        return new Movement(
                account,
                transfer,
                sequence,
                MovementType.CREDIT,
                amountCents,
                balanceAfterCents,
                createdAt
        );
    }

    public static Movement debit(Account account, Transfer transfer, long sequence, long amountCents, long balanceAfterCents, Instant createdAt) {
        return new Movement(
                account,
                transfer,
                sequence,
                MovementType.DEBIT,
                amountCents,
                balanceAfterCents,
                createdAt
        );
    }


}
