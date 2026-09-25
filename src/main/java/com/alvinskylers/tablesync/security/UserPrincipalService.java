package com.alvinskylers.tablesync.security;


import com.alvinskylers.tablesync.entity.User;
import com.alvinskylers.tablesync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPrincipalService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                    .orElseThrow(()-> new UsernameNotFoundException("account with such email not found")) ;
        return new UserPrincipal(user);
    }

}
