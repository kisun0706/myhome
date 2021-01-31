package com.megait.myhome;

import com.megait.myhome.domain.Member;
import com.megait.myhome.repository.MemberRepository;
import com.megait.myhome.util.EmailMessage;
import com.megait.myhome.util.EmailService;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @MockBean
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DisplayName("회원 가입 화면 보이는지 테스트")
    @Test
    void signupForm() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("view/user/signup"))
                .andExpect(model().attributeExists("signupForm"));
    }

    @DisplayName("회원 가입 - 입력값 오류")
    @Test
    void signupSubmit_with_wrong_input() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("email", "a@a.com")
                        .param("password", "aaaa")

                        // 약관 동의 체크 안하고...

                        .with(csrf()))  // Post 방식의 <form> 전송인 경우는 무조건 쓰자!
                                        // (타임리프 폼인 경우에는 csrf 토큰이 hidden 파라미터로 있다,)
                .andExpect(status().isOk())
                .andExpect(view().name("view/user/signup"))
                .andExpect(unauthenticated());

    }

    @DisplayName("회원 가입 - 입력값 정상")
    @Test
    void signupSubmit_with_correct_input() throws Exception {
        mockMvc.perform((post("/signup")
        .param("email", "issell@naver.com")
        .param("password", "1234")
        .param("agreeTermsOfService", "true")
        .with(csrf())))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"))
                .andExpect(authenticated());
        Assert.assertTrue(memberRepository.existsByEmail("a@a.a"));

        then(emailService).should().sendEmail(any(EmailMessage.class));
    }


    @DisplayName("인증 메일 확인 - 입력값 오류")
    @Test
    void checkEmailToken_with_wrong_input() throws Exception {
        mockMvc.perform(get("/check-email-token")
                .param("token", "qweqwe")
                .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("view/user/checked-email"))
                .andExpect(unauthenticated());
    }

    @DisplayName("인증 메일 확인 - 입력값 정상")
    @Test
    void checkEmailToken_with_correct_input() throws Exception {
        Member member = Member.builder()
                .email("test@test.com")
                .password("1234")
                .build();

        Member newMember = memberRepository.save(member);
        newMember.generateEmailCheckToken();

        mockMvc.perform(get("/check-email-token")
                .param("token", newMember.getEmailCheckToken())
                .param("email", newMember.getEmail()))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("email"))
                .andExpect(view().name("view/user/checked-email"))
                .andExpect(authenticated());
    }


    @DisplayName("로그인 확인 - 비밀번호 틀림")
    @Test
    void loginSubmit_with_wrong_password() throws Exception{
        // Given
        // 새 회원 추가
        // 이메일, 비번 넣기 (인코딩 해서 넣기)
        // memberRepository 에 저장
        Member member=Member.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin1234"))
                .build();
        Member newMember=memberRepository.save(member);

        // when
        // "/login" 요청을 날림
        // post 방식
        // username, password 파라미터 넣기
        mockMvc.perform(post("/login")
        .param("username", "admin@test.com")
        .param("password", "wrong_password")
        .with(csrf()))

        // then
        // error 라는 파라미터가 있니? or redirect 됐니?(정상일때)
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"))
        .andExpect(unauthenticated());
    }

    @DisplayName("로그인 확인 - 이메일, 비번 모두 맞음")
    @Test
    void loginSubmit_with_correct_email_and_password() throws Exception{

        Member member=Member.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin1234"))
                .build();
        Member newMember=memberRepository.save(member);

        mockMvc.perform(post("/login")
                .param("username", newMember.getEmail())
                .param("password", "admin1234")
                .with(csrf()))

                .andExpect(status().is3xxRedirection())  // 가입 성공하면 '/'로 리다이렉트 되도록 구현했었다
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated());
    }

}














