package com.agri.mapapp.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final AppUserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));
        Long orgId = (u.getOrg() != null) ? u.getOrg().getId() : null;
        return new UserPrincipal(u.getId(), u.getUsername(), u.getPassword(), u.getRole(), orgId);
    }
}
