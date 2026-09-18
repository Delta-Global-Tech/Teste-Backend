package com.marlonmachado.desafiotecnico.entities;


import com.marlonmachado.desafiotecnico.entities.enums.AccountStatus;
import jakarta.persistence.*;

import java.util.Objects;


@Entity
@Table(name = "accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_accounts_number", columnNames = "account_number")
        })
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 30)
    private String number;

    @Column(nullable = false)
    private Long balanceCents;

    @Column(nullable = false)
    private Long dailyLimitCents;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Version
    @Column(nullable = false)
    private long version;

    public Account() {
    }

    public Account(Long id, String number, Long balanceCents, Long dailyLimitCents, AccountStatus accountStatus, User user, long version) {
        this.id = id;
        this.number = number;
        this.balanceCents = balanceCents;
        this.dailyLimitCents = dailyLimitCents;
        this.accountStatus = accountStatus;
        this.user = user;
        this.version = version;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getBalanceCents() {
        return balanceCents;
    }

    public void setBalanceCents(Long balanceCents) {
        this.balanceCents = balanceCents;
    }

    public Long getDailyLimitCents() {
        return dailyLimitCents;
    }

    public void setDailyLimitCents(Long dailyLimitCents) {
        this.dailyLimitCents = dailyLimitCents;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public User getUser() {
        return user;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return Objects.equals(getId(), account.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}

