package com.example.productmanager.controller;

import com.example.productmanager.dto.CreateOrderDTO;
import com.example.productmanager.dto.OrderDTO;
import com.example.productmanager.entity.User;
import com.example.productmanager.repository.UserRepository;
import com.example.productmanager.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:8080", "http://127.0.0.1:8080"}, maxAge = 3600)
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('OM')")
    public ResponseEntity<List<OrderDTO>> getAllOrders(Authentication authentication) {
        System.out.println("Authorities: " + authentication.getAuthorities());
        List<OrderDTO> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(
            @PathVariable Long id,
            @RequestParam(required = false) String email,
            Authentication authentication) {
        
        // For authenticated users with permission
        if (authentication != null && authentication.isAuthenticated()) {
            // Check if user has admin/manager role or is the order owner
            boolean isAdminOrManager = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN") || 
                                         authority.getAuthority().equals("ROLE_OM"));
            
            if (isAdminOrManager) {
                OrderDTO order = orderService.getOrderById(id);
                return order != null
                        ? ResponseEntity.ok(order)
                        : ResponseEntity.notFound().build();
            }
            
            try {
                // Get user by authentication
                User user = userRepository.findByUsername(authentication.getName())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                
                // Get order if it belongs to the user
                OrderDTO order = orderService.getOrderByIdAndUserId(id, user.getId());
                return order != null
                        ? ResponseEntity.ok(order)
                        : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            } catch (Exception e) {
                logger.error("Error retrieving order by ID for authenticated user: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } 
        // For guest users who provide email
        else if (email != null && !email.isBlank()) {
            try {
                logger.info("Attempting to retrieve order #{} with email: {}", id, email);
                OrderDTO order = orderService.getOrderByIdAndEmail(id, email);
                return order != null
                        ? ResponseEntity.ok(order)
                        : ResponseEntity.notFound().build();
            } catch (Exception e) {
                logger.error("Error retrieving order by ID and email: {}", e.getMessage());
                return ResponseEntity.notFound().build();
            }
        }
        
        // If neither authenticated nor email provided
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OM') or (isAuthenticated() and #userId == @userRepository.findByUsername(authentication.name).get().id)")
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@PathVariable Long userId) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OM') or (isAuthenticated() and #email == authentication.name)")
    public ResponseEntity<List<OrderDTO>> getOrdersByEmail(@PathVariable String email) {
        List<OrderDTO> orders = orderService.getOrdersByEmail(email);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{statusName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OM')")
    public ResponseEntity<List<OrderDTO>> getOrdersByStatus(@PathVariable String statusName) {
        try {
            List<OrderDTO> orders = orderService.getOrdersByStatus(statusName);
            return ResponseEntity.ok(orders);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/customer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderDTO>> getCustomerOrders(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            
            List<OrderDTO> orders = orderService.getOrdersByUserId(user.getId());
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Error retrieving orders for authenticated user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody CreateOrderDTO createOrderDTO,
            Authentication authentication) {
        try {
            // If authenticated, associate with user
            if (authentication != null && authentication.isAuthenticated()) {
                Optional<User> user = userRepository.findByUsername(authentication.getName());
                if (user.isPresent() && createOrderDTO.getUserId() == null) {
                    createOrderDTO.setUserId(user.get().getId());
                }
            }
            
            OrderDTO createdOrder = orderService.createOrder(createOrderDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
        } catch (Exception e) {
            logger.error("Error creating order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status/{statusName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OM')")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id,
            @PathVariable String statusName) {
        try {
            OrderDTO updatedOrder = orderService.updateOrderStatus(id, statusName);
            return ResponseEntity.ok(updatedOrder);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        boolean deleted = orderService.deleteOrder(id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
} 