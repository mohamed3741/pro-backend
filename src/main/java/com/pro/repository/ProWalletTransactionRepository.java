package com.pro.repository;

import com.pro.model.ProWalletTransaction;
import com.pro.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProWalletTransactionRepository extends GenericRepository<ProWalletTransaction> {

    List<ProWalletTransaction> findByProIdOrderByCreatedAtDesc(Long proId);

    @Query("SELECT t FROM ProWalletTransaction t WHERE t.pro.id = :proId ORDER BY t.createdAt DESC")
    List<ProWalletTransaction> findTransactionsByProId(@Param("proId") Long proId);

    @Query("SELECT t FROM ProWalletTransaction t WHERE t.pro.id = :proId AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<ProWalletTransaction> findTransactionsByProIdAndDateRange(
            @Param("proId") Long proId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(t.amountMru) FROM ProWalletTransaction t WHERE t.pro.id = :proId AND t.type = 'CREDIT'")
    Long getTotalCreditsByProId(@Param("proId") Long proId);

    @Query("SELECT SUM(t.amountMru) FROM ProWalletTransaction t WHERE t.pro.id = :proId AND t.type = 'DEBIT'")
    Long getTotalDebitsByProId(@Param("proId") Long proId);
}
