package com.example.productmanager.controller;

import com.example.productmanager.dto.JwtResponse;
import com.example.productmanager.dto.LoginRequest;
import com.example.productmanager.dto.SignupRequest;
import com.example.productmanager.dto.CustomerLoginRequest;
import com.example.productmanager.dto.CustomerSignupRequest;
import com.example.productmanager.dto.CustomerAddressDTO;
import com.example.productmanager.entity.CustomerAddress;
import com.example.productmanager.entity.Role;
import com.example.productmanager.entity.User;
import com.example.productmanager.entity.Order;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserRepository;
import com.example.productmanager.repository.OrderRepository;
import com.example.productmanager.service.CustomerAddressService;
import com.example.productmanager.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.validation.FieldError;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:8080"}, 
    allowedHeaders = {"Authorization", "Content-Type", "Accept"}, 
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
    maxAge = 3600)
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private CustomerAddressService customerAddressService;

    public AuthController(AuthenticationManager authenticationManager,
                         UserRepository userRepository,
                         RoleRepository roleRepository,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);
            if (user != null) {
                return ResponseEntity.ok(new JwtResponse(jwt, "Bearer", user.getUsername(), user.getEmail()));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "User not found"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Invalid username or password"));
        }
    }
    
    @PostMapping("/customer-login")
    public ResponseEntity<?> authenticateCustomer(@Valid @RequestBody CustomerLoginRequest loginRequest) {
        try {
            logger.info("Processing customer login request for email: {}", loginRequest.getEmail());
            
            // Find user by email instead of username
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("No account found with this email"));
            
            // Create authentication token with username
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            
            // Include more user details in response
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("phone", user.getPhone());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("tokenType", "Bearer");
            response.put("user", userData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during customer login: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid email or password"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Username already exists"));
        }
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Email already exists"));
        }
        User user = new User();
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setEmail(signupRequest.getEmail());
        user.setUsername(signupRequest.getUsername());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        Role role = roleRepository.findByName(signupRequest.getRole()).orElse(null);
        if (role == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Role not found: " + signupRequest.getRole()));
        }
        user.setRoles(Collections.singleton(role));
        userRepository.save(user);
        return ResponseEntity.ok(Collections.singletonMap("message", "User registered successfully"));
    }
    
    @PostMapping("/customer-signup")
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody CustomerSignupRequest signupRequest) {
        try {
            logger.info("Processing customer signup request for email: {} with order ID: {}", 
                signupRequest.getEmail(), signupRequest.getOrderId());
            
            User user;
            boolean isNewUser = false;
            
            // Check if user with this email already exists
            Optional<User> existingUser = userRepository.findByEmail(signupRequest.getEmail());
            
            if (existingUser.isPresent()) {
                user = existingUser.get();
                // Check if this is a guest user (with placeholder password)
                if ("GUEST_USER_NO_LOGIN".equals(user.getPassword())) {
                    logger.info("Converting guest user to registered user: {}", user.getUsername());
                    // Update the guest user with real password
                    user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
                    user.setFirstName(signupRequest.getFirstName());
                    user.setLastName(signupRequest.getLastName());
                    user.setPhone(signupRequest.getPhone());
                } else {
                    // Real user with password already exists
                    logger.warn("Email {} is already in use by a registered user", signupRequest.getEmail());
                    return ResponseEntity.badRequest().body(Map.of("message", "Email is already in use by a registered account!"));
                }
            } else {
                // Create new user
                isNewUser = true;
                
                // Generate a unique username based on email
                String baseUsername = signupRequest.getEmail().split("@")[0];
                String username = baseUsername;
                int counter = 1;
                
                // Ensure username is unique
                while (userRepository.existsByUsername(username)) {
                    username = baseUsername + counter++;
                }
                logger.debug("Generated unique username: {}", username);
                
                user = new User();
                user.setUsername(username);
                user.setEmail(signupRequest.getEmail());
                user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
                user.setFirstName(signupRequest.getFirstName());
                user.setLastName(signupRequest.getLastName());
                user.setPhone(signupRequest.getPhone());
                
                // Assign customer role
                Role userRole = roleRepository.findByName("ROLE_USER")
                        .orElseGet(() -> {
                            logger.warn("Role ROLE_USER not found, creating it");
                            Role newRole = new Role();
                            newRole.setName("ROLE_USER");
                            newRole.setDescription("Regular customer role");
                            return roleRepository.save(newRole);
                        });
                
                Set<Role> roles = new HashSet<>();
                roles.add(userRole);
                user.setRoles(roles);
                logger.debug("Assigned role: {}", userRole.getName());
            }
            
            // Save the user
            user = userRepository.save(user);
            logger.info("Successfully saved user with ID: {}", user.getId());
            
            // If there's an order to link and it's not already linked to this user
            if (signupRequest.getOrderId() != null) {
                Optional<Order> orderOpt = orderRepository.findById(signupRequest.getOrderId());
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    
                    // Verify email matches
                    if (order.getEmail().equals(signupRequest.getEmail())) {
                        // Link order to user if not already linked
                        if (order.getUser() == null || !order.getUser().getId().equals(user.getId())) {
                            order.setUser(user);
                            orderRepository.save(order);
                            logger.info("Linked order #{} to user {}", order.getId(), user.getUsername());
                        }
                        
                        // Save address if requested
                        if (signupRequest.getSaveAddress() != null && signupRequest.getSaveAddress()) {
                            CustomerAddressDTO addressDTO = new CustomerAddressDTO();
                            addressDTO.setUserId(user.getId());
                            addressDTO.setAddress(order.getAddress());
                            addressDTO.setApartment(order.getApartment());
                            addressDTO.setCity(order.getCity());
                            addressDTO.setPostalCode(order.getPostalCode());
                            addressDTO.setIsDefault(true);
                            
                            customerAddressService.createAddress(addressDTO);
                            logger.info("Saved shipping address for user {}", user.getUsername());
                        }
                    } else {
                        logger.warn("Email mismatch when linking order #{} to user {}", order.getId(), user.getUsername());
                    }
                }
            }
            
            // Authenticate the user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), signupRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = tokenProvider.generateToken(authentication);
            
            // Include more user details in response
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("firstName", user.getFirstName());
            userData.put("lastName", user.getLastName());
            userData.put("phone", user.getPhone());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("tokenType", "Bearer");
            response.put("user", userData);
            
            String actionType = isNewUser ? "registered" : "converted from guest to registered user";
            logger.info("Customer {} successfully {} and authenticated", user.getUsername(), actionType);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error during customer registration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Error during registration: " + e.getMessage()));
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> body = new HashMap<>();
        body.put("id", user.getId());
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        body.put("firstName", user.getFirstName());
        body.put("lastName", user.getLastName());
        body.put("phone", user.getPhone());
        body.put("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
        return ResponseEntity.ok(body);
    }

    /**
     * GET /api/auth/check-admin : Check if the current user has ADMIN role
     * 
     * @param authentication the current authentication
     * @return the ResponseEntity with status 200 (OK) and with body containing the admin status
     */
    @GetMapping("/check-admin")
    public ResponseEntity<?> checkAdminStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            // Not authenticated
            return ResponseEntity.ok(Map.of("isAuthenticated", false, "isAdmin", false));
        }
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        
        // Log the check
        logger.info("Admin check for user {}: {}", authentication.getName(), isAdmin);
        
        return ResponseEntity.ok(Map.of(
            "isAuthenticated", true, 
            "isAdmin", isAdmin,
            "username", authentication.getName()
        ));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        logger.warn("Validation errors: {}", fieldErrors);
        // Return structured error including field errors
        return Map.of("message", "Validation failed", "errors", fieldErrors); 
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RuntimeException.class)
    public Map<String, String> handleRuntimeExceptions(RuntimeException ex) {
        logger.error("Runtime exception: {}", ex.getMessage(), ex);
        return Map.of("message", ex.getMessage());
    }

    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null) {
                Map<String, Object> userDetails = new HashMap<>();
                userDetails.put("username", user.getUsername());
                userDetails.put("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()));
                return ResponseEntity.ok(userDetails);
            }
        }
        return ResponseEntity.status(401).body(Collections.singletonMap("message", "Not authenticated"));
    }

    @PostMapping("/customer/signup")
    public ResponseEntity<?> customerSignup(@RequestBody CustomerSignupRequest signupRequest) {
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Email already exists"));
        }
        User user = new User();
        user.setFirstName(signupRequest.getFirstName());
        user.setLastName(signupRequest.getLastName());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setPhone(signupRequest.getPhone());
        Role role = roleRepository.findByName("ROLE_USER").orElse(null);
        if (role == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Role not found: ROLE_USER"));
        }
        user.setRoles(Collections.singleton(role));
        userRepository.save(user);
        if (signupRequest.getOrderId() != null) {
            Order order = orderRepository.findById(signupRequest.getOrderId()).orElse(null);
            if (order != null) {
                order.setUser(user);
                orderRepository.save(order);
            }
        }
        return ResponseEntity.ok(Collections.singletonMap("message", "Customer registered successfully"));
    }
} 