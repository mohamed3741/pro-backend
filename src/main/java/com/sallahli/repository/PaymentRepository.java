package com.sallahli.repository;

import com.sallahli.model.Payment;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends GenericRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.onlineTransaction WHERE p.onlineTransaction.id = :onlineTransactionId")
    Payment findByOnlineTransactionId(@Param("onlineTransactionId") Long onlineTransactionId);
}
