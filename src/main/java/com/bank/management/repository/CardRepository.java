package com.bank.management.repository;

import com.bank.management.entity.Card;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    @EntityGraph(attributePaths = "user")
    @Query("select c from Card c where c.id = :id")
    Optional<Card> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = "user")
    @Query("select c from Card c")
    Page<Card> findAllWithUser(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Card> findByUser_Id(Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select c from Card c join fetch c.user where c.id = :id")
    Optional<Card> findByIdForUpdate(@Param("id") Long id);
}
