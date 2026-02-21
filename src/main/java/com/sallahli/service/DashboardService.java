package com.sallahli.service;

import com.sallahli.dto.sallahli.CustomerRequestDTO;
import com.sallahli.dto.sallahli.DashboardStatsDTO;
import com.sallahli.mapper.CustomerRequestMapper;
import com.sallahli.model.CustomerRequest;
import com.sallahli.model.Enum.RequestStatus;
import com.sallahli.repository.ClientRepository;
import com.sallahli.repository.CustomerRequestRepository;
import com.sallahli.repository.ProRepository;
import com.sallahli.repository.ProWalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final ClientRepository clientRepository;
    private final ProRepository proRepository;
    private final CustomerRequestRepository customerRequestRepository;
    private final ProWalletTransactionRepository proWalletTransactionRepository;
    private final CustomerRequestMapper customerRequestMapper;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getStats() {
        Long totalCustomers = clientRepository.count();
        Long totalProviders = proRepository.count();
        Long totalRequests = customerRequestRepository.count();

        Long totalRevenueRaw = proWalletTransactionRepository.getTotalRevenue();
        BigDecimal totalRevenue = totalRevenueRaw != null ? BigDecimal.valueOf(totalRevenueRaw) : BigDecimal.ZERO;

        Long pendingRequests = customerRequestRepository.countByStatus(RequestStatus.BROADCASTED);
        Long completedRequests = customerRequestRepository.countByStatus(RequestStatus.DONE);

        LocalDateTime startOfMonth = LocalDateTime.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        Long newCustomersThisMonth = clientRepository.countNewClientsSince(startOfMonth);

        return DashboardStatsDTO.builder()
                .totalCustomers(totalCustomers)
                .totalProviders(totalProviders)
                .totalRequests(totalRequests)
                .totalRevenue(totalRevenue)
                .pendingRequests(pendingRequests)
                .completedRequests(completedRequests)
                .newCustomersThisMonth(newCustomersThisMonth)
                .build();
    }

    @Transactional(readOnly = true)
    public List<CustomerRequestDTO> getRecentRequests(int limit) {
        List<CustomerRequest> requests = customerRequestRepository.findRecentRequests(PageRequest.of(0, limit));
        return customerRequestMapper.toDtos(requests);
    }
}
