package com.cadac.stone_inscription.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cadac.stone_inscription.entity.UserAuth;
import com.cadac.stone_inscription.repository.UserAuthRepository;

// import com.cdac.aihkb.Dao.TAssignRolesRepo;
// import com.cdac.aihkb.Dao.TEmployeeRepo;
// import com.cdac.aihkb.Dao.TRoleRepo;
// import com.cdac.aihkb.Entity.TEmployee;


@Service
public class StoneInscriptionUserDetailservice implements UserDetailsService {

    @Autowired
    private UserAuthRepository userAuthRepository;


    


@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

    // TEmployee tEmployee = tEmployeeRepo.findByEmail(username);
    UserAuth user = userAuthRepository.findByEmail(username);

    System.out.println("load userName Called");
    
   

    if(user != null ){

    // ArrayList<TAssignRoles> tAssignRoles = tAssignRolesRepo.findByEmail(username);
         


    // ArrayList<SimpleGrantedAuthority> roles = new ArrayList<>();

    // tAssignRoles.forEach(role ->{
        
    //   Optional<TRole> trole = tRoleRepo.findById(role.getRoleId()); 

      
    //   roles.add(new SimpleGrantedAuthority(trole.get().getRoleName()));  

    // });

    List<SimpleGrantedAuthority> authorities = user.getRoles() == null
            ? new ArrayList<>()
            : user.getRoles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toCollection(ArrayList::new));

    return new User(user.getEmail(),user.getPasswordHash(), authorities); 

  

    }
    else {
        throw new UsernameNotFoundException(username);
    }

      
}

}
