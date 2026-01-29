package com.sallahli.repository;

import com.sallahli.model.OnlineTransaction;
import com.sallahli.repository.generic.GenericRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnlineTransactionRepository extends GenericRepository<OnlineTransaction> {

    OnlineTransaction findByOperationId(String operationId);
}
