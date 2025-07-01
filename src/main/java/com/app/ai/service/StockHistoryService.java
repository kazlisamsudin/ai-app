package com.app.ai.service;

import com.app.ai.model.Product;
import com.app.ai.model.StockHistory;
import com.app.ai.repository.StockHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockHistoryService {

    @Autowired
    private StockHistoryRepository stockHistoryRepository;

    /**
     * Record a stock change in the audit trail
     */
    public void recordStockChange(Product product, Integer previousQuantity, Integer newQuantity,
                                 StockHistory.ChangeType changeType, String reason, Long orderId) {
        String currentUser = getCurrentUsername();

        StockHistory history = new StockHistory(product, previousQuantity, newQuantity,
                                              changeType, reason, currentUser);
        if (orderId != null) {
            history.setOrderId(orderId);
        }

        stockHistoryRepository.save(history);
    }

    /**
     * Record a stock change without order reference
     */
    public void recordStockChange(Product product, Integer previousQuantity, Integer newQuantity,
                                 StockHistory.ChangeType changeType, String reason) {
        recordStockChange(product, previousQuantity, newQuantity, changeType, reason, null);
    }

    /**
     * Get stock history for a specific product
     */
    public List<StockHistory> getProductStockHistory(Long productId) {
        return stockHistoryRepository.findByProductIdOrderByChangeDateDesc(productId);
    }

    /**
     * Get paginated stock history for a specific product
     */
    public Page<StockHistory> getProductStockHistory(Long productId, Pageable pageable) {
        return stockHistoryRepository.findByProductIdOrderByChangeDateDesc(productId, pageable);
    }

    /**
     * Get recent stock changes across all products
     */
    public Page<StockHistory> getAllStockHistory(Pageable pageable) {
        return stockHistoryRepository.findAllByOrderByChangeDateDesc(pageable);
    }

    /**
     * Get stock changes by type
     */
    public List<StockHistory> getStockHistoryByType(StockHistory.ChangeType changeType) {
        return stockHistoryRepository.findByChangeTypeOrderByChangeDateDesc(changeType);
    }

    /**
     * Get stock changes within date range
     */
    public List<StockHistory> getStockHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return stockHistoryRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Get stock changes by user
     */
    public List<StockHistory> getStockHistoryByUser(String username) {
        return stockHistoryRepository.findByChangedByOrderByChangeDateDesc(username);
    }

    /**
     * Get stock change summary for a product
     */
    public Object[] getStockChangeSummary(Long productId) {
        return stockHistoryRepository.getStockChangeSummary(productId);
    }

    /**
     * Helper method to get current username
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}
