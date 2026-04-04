package com.example.productmanager.config;

import com.example.productmanager.entity.Role;
import com.example.productmanager.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RoleDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleDataInitializer.class);

    private final RoleRepository roleRepository;

    public RoleDataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureRole("ROLE_USER", "Regular customer role");
        ensureRole("ROLE_ADMIN", "Administrator");
        ensureRole("ROLE_PM", "Product Manager");
        ensureRole("ROLE_OM", "Order Manager");
        ensureRole("ROLE_ORDER_MANAGER", "Order Manager");
    }

    private void ensureRole(String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            roleRepository.save(role);
            log.info("Created missing role: {}", name);
        }
    }
}
