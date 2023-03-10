package com.example.airplanning.configuration;

import com.example.airplanning.configuration.login.CustomOauth2UserService;
import com.example.airplanning.configuration.login.UserDetail;
import com.example.airplanning.domain.enum_class.UserRole;
import com.example.airplanning.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CustomOauth2UserService customOauth2UserService;
    private final UserService userService;
    private final AccessDeniedHandler accessDeniedHandler;

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf().disable()
                .cors().and()
                .authorizeRequests()
                .antMatchers("/upload").permitAll()
                .antMatchers("/api/order/**").permitAll()
                .antMatchers("/api/v1/hello").authenticated()
                .antMatchers(HttpMethod.GET, "/boards/write").authenticated()
                .antMatchers(HttpMethod.POST, "/api/boards/write").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/boards/**").authenticated()
                .antMatchers(HttpMethod.DELETE, "/api/boards/**").authenticated()
                .antMatchers(HttpMethod.GET, "/users/mypage").authenticated()
//                .antMatchers("/boards/rankup/**").authenticated()
                .antMatchers(HttpMethod.GET, "/reviews/write").authenticated()
                .antMatchers(HttpMethod.POST, "/reviews").authenticated()
                .antMatchers(HttpMethod.GET, "/users/set-nickname").authenticated()
                .antMatchers("/boards/portfolio/write").hasAuthority("PLANNER")
                .antMatchers("/boards/portfolio/**").authenticated()
                .antMatchers( "/boards/write").authenticated()
                .antMatchers( HttpMethod.POST,"/boards/**").authenticated()
                .antMatchers(HttpMethod.GET, "/plans/write").authenticated()
                .antMatchers(HttpMethod.POST, "/plans").authenticated()
                .antMatchers("/chat/**").authenticated()
                .anyRequest().permitAll()
                .and()
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)
                .and()

                // ??? ????????? ??????
                .formLogin()
                .loginPage("/users/login")      // ???????????? ????????? ????????? ?????? ??????. ????????? ??????????????? ???????????? ???????????? ???
                .failureUrl("/api/login2") //?????? ??? ?????? ?????????
                .usernameParameter("userName")  // html ?????? "userName"??? ???????????? ????????? ???????????? ???
                .passwordParameter("password")  // html ?????? "password"??? ???????????? ????????? ???????????? ???
                .successHandler(                // ????????? ?????????, ?????? ???????????? 3600???,  ?????????????????? ??? ???????????? ??????
                        new AuthenticationSuccessHandler() {
                            @Override
                            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                                UserDetail userDetail = (UserDetail) authentication.getPrincipal();
                                if (!userDetail.isAccountNonLocked()) {
                                    throw new LockedException("?????? ?????? ???????????????.");
                                }

                                HttpSession session = request.getSession();
                                session.setMaxInactiveInterval(3600);
                                response.setContentType("text/html");
                                PrintWriter out = response.getWriter();
                                String loginUserNickname = userService.findUser(authentication.getName()).getNickname();
                                out.println("<script>alert('" + loginUserNickname+ "??? ???????????????!'); location.href='/';</script>");
                                out.flush();
                            }
                        }
                )
                .failureHandler(
                        new AuthenticationFailureHandler() {
                            @Override
                            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                                String loginFailMessage = "";
                                if(exception instanceof AuthenticationServiceException) {
                                    loginFailMessage = "???????????????. ???????????? ????????? ??????????????????.";
                                } else if(exception instanceof BadCredentialsException || exception instanceof UsernameNotFoundException) {
                                    response.sendRedirect("/users/login?fail");
                                    return;
                                } else if(exception instanceof DisabledException) {
                                    loginFailMessage = "?????? ????????? ??? ?????? ???????????????.";
                                } else if(exception instanceof LockedException) {
                                    loginFailMessage = "?????? ?????? ???????????????.";
                                } else if(exception instanceof AccountExpiredException) {
                                    loginFailMessage = "?????? ????????? ???????????????.";
                                } else if(exception instanceof CredentialsExpiredException) {
                                    loginFailMessage = "??????????????? ????????? ???????????????.";
                                }

                                response.setContentType("text/html");
                                PrintWriter out = response.getWriter();
                                out.println("<script>alert('" + loginFailMessage + "'); location.href='/users/login';</script>");
                                out.flush();
                            }
                        }
                )
                .permitAll()
                .and()

                // ????????????
                .logout()
                .logoutUrl("/users/logout")
                .invalidateHttpSession(true)
                .logoutSuccessHandler(
                        new LogoutSuccessHandler() {
                            @Override
                            public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                                response.setContentType("text/html");
                                PrintWriter out = response.getWriter();
                                out.println("<script>alert('???????????? ????????????.'); location.href='/';</script>");
                                out.flush();
                            }
                        }
                )
                .and()

                // OAuth2 ?????? ?????????
                .oauth2Login()
                .loginPage("/users/login")
                .failureUrl("/users/login")
                .successHandler(                // ????????? ?????????, ?????? ???????????? 3600???,  ?????????????????? ??? ???????????? ??????
                        new AuthenticationSuccessHandler() {
                            @Override
                            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                                UserDetail userDetail = (UserDetail) authentication.getPrincipal();
                                if (!userDetail.isAccountNonLocked()) {
                                    throw new LockedException("?????? ?????? ???????????????.");
                                }

                                HttpSession session = request.getSession();
                                session.setMaxInactiveInterval(3600);
                                String loginUserNickname = userService.findUser(authentication.getName()).getNickname();
                                if(loginUserNickname != null) {
                                    response.setContentType("text/html");
                                    PrintWriter out = response.getWriter();
                                    out.println("<script>alert('" + loginUserNickname+ "??? ???????????????!'); location.href='/';</script>");
                                    out.flush();
                                } else {
                                    response.sendRedirect("/");
                                }
                            }
                        }
                )
                .failureHandler(
                        new AuthenticationFailureHandler() {
                            @Override
                            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                                String loginFailMessage = "";
                                if(exception instanceof AuthenticationServiceException) {
                                    loginFailMessage = "???????????????. ???????????? ????????? ??????????????????.";
                                } else if(exception instanceof BadCredentialsException || exception instanceof UsernameNotFoundException) {
                                    response.sendRedirect("/users/login?fail");
                                    return;
                                } else if(exception instanceof DisabledException) {
                                    loginFailMessage = "?????? ????????? ??? ?????? ???????????????.";
                                } else if(exception instanceof LockedException) {
                                    loginFailMessage = "?????? ?????? ???????????????.";
                                } else if(exception instanceof AccountExpiredException) {
                                    loginFailMessage = "?????? ????????? ???????????????.";
                                } else if(exception instanceof CredentialsExpiredException) {
                                    loginFailMessage = "??????????????? ????????? ???????????????.";
                                }

                                response.setContentType("text/html");
                                PrintWriter out = response.getWriter();
                                out.println("<script>alert('" + loginFailMessage + "'); location.href='/users/login';</script>");
                                out.flush();
                            }
                        })
                .userInfoEndpoint().userService(customOauth2UserService)
                .and()
                .and()
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(true)
                        .sessionRegistry(sessionRegistry())
                        .expiredSessionStrategy(new SessionInformationExpiredStrategy() {
                            @Override
                            public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException, ServletException {
                                HttpServletResponse response = event.getResponse();
                                response.setContentType("text/html");
                                PrintWriter out = response.getWriter();
                                out.println("<script>alert('????????? ?????????????????????'); location.href='/users/login';</script>");
                                out.flush();
                            }
                        })
                )
                .build();
    }
}
