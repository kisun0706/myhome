package com.megait.myhome.config;

import com.megait.myhome.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity   // 내가 시큐리티 설정을 직접 하겠다.
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final MemberService memberService;
    private final DataSource dataSource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()

                // 다음 목록은 로그인 없이도 요청 가능
                .mvcMatchers("/",
                        "/login",
                        "/signup",
                        "/check-email",
                        "/check-email-token",
                        "/change-password",
                        "/reset-password",
                        "/store/**").permitAll()

                // 다음 목록은 get 만 요청 가능
                .mvcMatchers(HttpMethod.GET, "/item/*").permitAll()

//                .antMatchers("/css/**", "/images/**", "/js/**", "**/favicon.ico").permitAll()

                // 나머지 요청은 로그인 해야만 요청 가능
                .anyRequest().authenticated();

        http.formLogin()
                .loginPage("/login")    // 이곳이 로그인 경로다!
                .permitAll();           // 여기는 누구든 요청 가능하다.

        http.logout()
                // .logoutUrl("/logout") // 이 경로는 이미 기본값이다.
                .invalidateHttpSession(true)
                .logoutSuccessUrl("/"); // 로그아웃 후 다음 페이지 --> index 페이지로..

        http.rememberMe()
                .userDetailsService(memberService)
                .tokenRepository(tokenRepository());

    }

    @Bean
    public PersistentTokenRepository tokenRepository() {
        JdbcTokenRepositoryImpl jdbcTokenRepository=new JdbcTokenRepositoryImpl();
        jdbcTokenRepository.setDataSource(dataSource);

        return jdbcTokenRepository;
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }
}
