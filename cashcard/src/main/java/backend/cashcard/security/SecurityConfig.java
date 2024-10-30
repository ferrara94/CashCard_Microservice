package backend.cashcard.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * In the latest versions of Spring Security (5.0 and above), WebSecurityConfigurerAdapter has been removed. Instead,
 * you should configure Spring Security by directly using SecurityFilterChain and @Bean methods in your configuration
 * class.
 **/

//@EnableWebSecurity
@Configuration
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //any request to /cashcards/** should require authentication.
                .authorizeHttpRequests(request -> request
                                .requestMatchers("/h2-console/**").permitAll() // Allow all to access H2
                                .requestMatchers("/cashcards/**").hasRole("CARD-OWNER") //we enable the RBAC-based authorization!
                                //request.requestMatchers("/cashcards/**").authenticated()
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder){
        User.UserBuilder users = User.builder();
        UserDetails felix = users
                                .username("felix")
                                .password(passwordEncoder.encode("abc123"))
                                .roles("CARD-OWNER")
                                .build();

        UserDetails userWhoOwnsNoCards = users
                                            .username("user-owns-no-cards")
                                            .password(passwordEncoder.encode("qrs456"))
                                            .roles("NON-OWNER")
                                            .build();

        UserDetails kumar = users
                                            .username("kumar2")
                                            .password(passwordEncoder.encode("xyz789"))
                                            .roles("CARD-OWNER")
                                            .build();

        return new InMemoryUserDetailsManager(felix, userWhoOwnsNoCards, kumar);
    }

}
