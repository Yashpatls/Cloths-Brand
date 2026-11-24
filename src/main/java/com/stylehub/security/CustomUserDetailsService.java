package com.stylehub.security;
import com.stylehub.repo.UserRepository; import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service; import org.springframework.security.core.authority.SimpleGrantedAuthority;
@Service
public class CustomUserDetailsService implements UserDetailsService {
 private final UserRepository users;
 public CustomUserDetailsService(UserRepository users){this.users=users;}
 @Override public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
  var u=users.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("Account not found"));
  return User.withUsername(u.getEmail()).password(u.getPassword()).authorities(new SimpleGrantedAuthority("ROLE_"+u.getRole().name())).build();
 }
}
