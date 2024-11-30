package zzk.townshipscheduler.backend;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import zzk.townshipscheduler.backend.persistence.dao.AppUserEntityRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserEntityRepository appUserEntityRepository;

    public UserDetailsServiceImpl(AppUserEntityRepository appUserEntityRepository) {
        this.appUserEntityRepository = appUserEntityRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return appUserEntityRepository.findByUsername(username);
    }

}
