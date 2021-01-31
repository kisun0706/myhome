package com.megait.myhome.service;

import com.megait.myhome.config.AppProperties;
import com.megait.myhome.domain.Address;
import com.megait.myhome.domain.Item;
import com.megait.myhome.domain.Member;
import com.megait.myhome.domain.MemberType;
import com.megait.myhome.form.SignupForm;
import com.megait.myhome.form.SignupFormValidator;
import com.megait.myhome.repository.MemberRepository;
import com.megait.myhome.util.EmailMessage;
import com.megait.myhome.util.EmailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class MemberService implements UserDetailsService {

    //private final JavaMailSender javaMailSender;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    private final MemberRepository memberRepository;
    private final SignupFormValidator signupFormValidator;
    private final PasswordEncoder passwordEncoder;

    @InitBinder("signupForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(signupFormValidator);
    }

    // id: a@a.a
    // pw: 1234
    @PostConstruct
    public void createTestUser(){
        Member member=Member.builder()
                .email("a@a.a")
                .password(passwordEncoder.encode("1234"))
                .build();
        member.generateEmailCheckToken();
        memberRepository.save(member);
    }

    public Member saveNewMember(SignupForm signupForm) {
        Member member = Member.builder()
                .email(signupForm.getEmail())
                .password(passwordEncoder.encode(signupForm.getPassword()))
                .address(Address.builder()
                        .city(signupForm.getCity())
                        .street(signupForm.getStreet())
                        .zip(signupForm.getZipcode())
                        .build())
                .build();
        Member newMember = memberRepository.save(member);
        return newMember;
    }

    public void sendSignupConfirmEmail(Member newMember) {
        sendEmail(newMember, "My Book Store - 회원 가입 인증","/check-email-token");
    }

    public void login(Member member) {
        // Username, password 정보를 가지고
        // 인증 요청을 보냄. 발생한 인증 토큰을 가지고
        // SecurityContext 에 인증 토큰 저장 (로그인된 유저 추가)

        MemberUser memberUser = new MemberUser(member);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                        memberUser, //member.getEmail(), // Principal
                        memberUser.getMember().getPassword(), //member.getPassword(),
                        memberUser.getAuthorities() //List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(token);
    }

    @Transactional
    public Member processNewMember(SignupForm signupForm) {

        Member newMember = saveNewMember(signupForm);
        newMember.generateEmailCheckToken();
        newMember.setType(MemberType.USER);
        sendSignupConfirmEmail(newMember);
        return newMember;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member=memberRepository.findByEmail(email);
        if (member == null) {
            throw new UsernameNotFoundException(email);
        }

        return new MemberUser(member);
    }

    public void sendEmailForNewPassword(String email){
        Member member=memberRepository.findByEmail(email);
        if (member == null) {
            return;
        }
        sendEmail(member, "My Book Store - 비밀번호 재설정","/reset-password");
    }

    @Transactional
    public boolean checkToken(String token, String email) {
        Member member=memberRepository.findByEmail(email);

        if(token.equals(member.getEmailCheckToken())){
            return true;
        }
        return false;
    }

    @Transactional  // Repository 외부에서 엔티티.setXXX() 가 호출되는 경우라면 무조건 @Transactional 추가
    public void processResetPassword(String email, String password) {
        Member member = memberRepository.findByEmail(email);
        member.setPassword(passwordEncoder.encode(password));
    }

    @Transactional
    public List<Item> getLikeList(Member member) {
        return memberRepository.findByEmail(member.getEmail()).getLikes();
    }


    private void sendEmail(Member member, String subject, String url){
        Context context=new Context();
        context.setVariable("link", url + "?token="+member.getEmailCheckToken()+"&email="+member.getEmail());
        context.setVariable("host", appProperties.getHost());
        context.setVariable("linkName", "이메일 인증하기");
        context.setVariable("message", "서비스 이용을 위해 링크를 클릭하세요.");

        String html=templateEngine.process("mail/simple-link", context);

        EmailMessage emailMessage=EmailMessage.builder()
                .to(member.getEmail())
                .subject(subject)
                .message(html)
                .build();
        emailService.sendEmail(emailMessage);
    }

}









