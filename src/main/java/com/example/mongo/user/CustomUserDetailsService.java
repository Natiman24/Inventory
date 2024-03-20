package com.example.mongo.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + phone));

        System.out.println("phone "+user.getPhoneNumber());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getPhoneNumber())
                .password(user.getPassword())
                .build();
    }
}
