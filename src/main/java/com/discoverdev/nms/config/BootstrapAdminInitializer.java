package com.discoverdev.nms.config;

import com.discoverdev.nms.user.Locale;
import com.discoverdev.nms.user.Role;
import com.discoverdev.nms.user.User;
import com.discoverdev.nms.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties props;

    public BootstrapAdminInitializer(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        BootstrapAdminProperties props
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (props.email() == null || props.email().isBlank()) {
            log.warn("Bootstrap super admin email not configured; skipping seed");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(props.email())) {
            log.info("Bootstrap super admin {} already exists; skipping seed", props.email());
            return;
        }

        User superAdmin = User.builder()
            .email(props.email())
            .passwordHash(passwordEncoder.encode(props.password()))
            .fullName(props.fullName())
            .role(Role.SUPER_ADMIN)
            .locale(Locale.EN)
            .enabled(true)
            .build();
        userRepository.save(superAdmin);

        log.info("Bootstrap super admin seeded: {}", props.email());
    }
}
