package com.gamerecs.back.service;

import com.gamerecs.back.model.User;
import com.gamerecs.back.repository.UserRepository;
import com.gamerecs.back.security.CustomUserDetails;
import com.gamerecs.back.util.UsernameNormalizer;
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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalizedUsername = UsernameNormalizer.normalize(username);
        User user = userRepository.findByUsername(normalizedUsername)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalizedUsername));
        
        return new CustomUserDetails(
            user.getUsername(),
            user.getPasswordHash(),
            user.isEmailVerified(),
            user.getUserId()
        );
    }
} 
