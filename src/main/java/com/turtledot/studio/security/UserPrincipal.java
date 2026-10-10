package com.turtledot.studio.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.turtledot.studio.user.Role;
import com.turtledot.studio.user.User;
import com.turtledot.studio.user.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private Long id;
    private Long studioId;
    private String email;
    @JsonIgnore
    private String password;
    private String fullName;
    private Role role;
    private UserStatus status;
    private Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, Long studioId, String email, String password, String fullName, Role role, UserStatus status, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.studioId = studioId;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new UserPrincipal(
                user.getId(),
                user.getStudioId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                authorities
        );
    }

    public Long getId() { return id; }
    public Long getStudioId() { return studioId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return password; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return status == UserStatus.ACTIVE; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return status == UserStatus.ACTIVE; }
}
