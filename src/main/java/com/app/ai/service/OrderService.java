package com.app.ai.service;

import com.app.ai.model.Order;
import com.app.ai.model.OrderItem;
import com.app.ai.model.User;
import com.app.ai.repository.OrderRepository;
import com.app.ai.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> findOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    public Page<Order> findPaginatedOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public Page<Order> findOrdersByUser(User user, Pageable pageable) {
        return orderRepository.findByUser(user, pageable);
    }

    public Page<Order> findOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    public List<Order> findUserOrderHistory(User user) {
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    public Order updateOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(newStatus);

            // Set timestamps based on status
            LocalDateTime now = LocalDateTime.now();
            switch (newStatus) {
                case SHIPPED:
                    order.setShippedDate(now);
                    break;
                case DELIVERED:
                    order.setDeliveredDate(now);
                    break;
            }

            return orderRepository.save(order);
        }
        throw new RuntimeException("Order not found with id: " + orderId);
    }

    // Analytics Methods
    public BigDecimal getTotalRevenueByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = orderRepository.getTotalRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public Long getOrderCountFromDate(LocalDateTime date) {
        return orderRepository.countOrdersFromDate(date);
    }

    public List<Object[]> getOrderStatusCounts() {
        return orderRepository.getOrderStatusCounts();
    }

    public List<Object[]> getMonthlyOrderStats(int year) {
        return orderRepository.getMonthlyOrderStats(year);
    }

    public List<Object[]> getMostPopularProducts() {
        return orderItemRepository.findMostPopularProducts();
    }

    public List<Object[]> getTopRevenueProducts() {
        return orderItemRepository.findTopRevenueProducts();
    }

    // Order calculation methods
    public BigDecimal calculateOrderTotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
