package io.github.joaomnz.bettracker.security;

import io.github.joaomnz.bettracker.model.Bettor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class BettorDetails implements UserDetails {
    private final Bettor bettor;

    public BettorDetails(Bettor bettor) {
        this.bettor = bettor;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + bettor.getType().name()));
    }

    @Override
    public String getPassword() {
        return bettor.getPassword();
    }

    @Override
    public String getUsername() {
        return bettor.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
