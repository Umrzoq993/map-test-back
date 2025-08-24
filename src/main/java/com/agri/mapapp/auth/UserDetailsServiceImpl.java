package com.agri.mapapp.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final AppUserRepository repo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found"));

        // Organization -> OrganizationUnit ga moslashtirildi
        Long orgId = (u.getOrgUnit() != null) ? u.getOrgUnit().getId() : null;

        return new UserPrincipal(
                u.getId(),
                u.getUsername(),
                u.getPassword(),
                u.getRole(),
                orgId
        );
    }
}
