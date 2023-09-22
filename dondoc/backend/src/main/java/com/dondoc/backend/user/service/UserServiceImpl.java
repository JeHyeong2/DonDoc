package com.dondoc.backend.user.service;

import com.dondoc.backend.common.exception.NotFoundException;
import com.dondoc.backend.common.jwt.JwtTokenProvider;
import com.dondoc.backend.common.jwt.TokenDto;
import com.dondoc.backend.common.jwt.model.UserDetailsImpl;
import com.dondoc.backend.common.utils.EncryptionUtils;
import com.dondoc.backend.user.dto.user.*;
import com.dondoc.backend.user.entity.User;
import com.dondoc.backend.user.repository.AccountRepository;
import com.dondoc.backend.user.repository.FriendRepository;
import com.dondoc.backend.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import java.util.NoSuchElementException;

@Slf4j
@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;

    private final EncryptionUtils encryptionUtils;

    private final JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder;

    // 유저의 정보 로직을 담은 객체
    private final UserDetailsService userDetailsService;

    private final FriendRepository friendRepository;

    private final AccountRepository accountRepository;

    // 회원가입
    @Override
    public SignUpDto.Response signUp(SignUpDto.Request signUpDto) throws Exception {
        // 인증여부 판별
        if(!signUpDto.isCertification()){
            return SignUpDto.Response.builder()
                    .msg("인증이 완료되지 않았습니다.")
                    .success(false)
                    .build();
        }

        // salt 생성
        String salt  = encryptionUtils.makeSalt();

        // User 객체 생성
        User user = User.builder()
                        .phoneNumber(signUpDto.getPhoneNumber())
                        .name(signUpDto.getName())
                        .password(passwordEncoder.encode(signUpDto.getPassword() + salt))
                        .email(signUpDto.getEmail())
                        .nickName(signUpDto.getNickName())
                        .salt(salt)
                        .build();

        userRepository.save(user);

        return SignUpDto.Response.builder()
                .msg("회원가입이 성공적으로 완료되었습니다.")
                .success(true)
                .build();
    }

    @Override
    public SignInDto.Response signIn(SignInDto.Request req) throws Exception {
        // 사용자 존재 여부 파악
        User user = userRepository.findByPhoneNumber(req.getPhoneNumber())
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        log.info("user {}",user.getName());

        // 비밀번호 검증
        if(!passwordEncoder.matches(req.getPassword() + user.getSalt() , user.getPassword())){
            throw new NoSuchElementException("비밀번호가 일치하지 않습니다.");
        }

        log.info("certification");

        // 토큰 Dto
        TokenDto tokenDto = TokenDto.builder()
                .name(user.getName())
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .build();


        // 토큰 생성 메서드
        String accessToken = jwtTokenProvider.createAccessToken(tokenDto);
        Cookie refreshToken = jwtTokenProvider.createRefreshToken(tokenDto);

        // 토큰 등록
        log.info("access : {}" , accessToken);
        log.info("refresh : {}" , refreshToken.getValue());

        // DB RefreshToken 저장
        user.setRefreshToken(refreshToken.getValue());
        userRepository.save(user);

        // User 인증 정보 불러오기(유효한 accessToken)
        UserDetailsImpl userDetails = (UserDetailsImpl)userDetailsService.loadUserByUsername(user.getId().toString());
        log.info("핸드폰번호 = {}", userDetails.getUsername());

        // User 정보 등록
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        log.info(usernamePasswordAuthenticationToken.getName());


        return SignInDto.Response.builder()
                .success(true)
                .msg("정상적으로 로그인 되었습니다.")
                .name(user.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public CertificationDto.Response sendSMS(String phoneNumber) {
        return null;
    }

    @Override
    public FindPasswordDto.Response findPassword(FindPasswordDto.Request req) {
        User user = userRepository.findByPhoneNumber(req.getPhoneNumber())
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        // salt 생성
        String salt  = encryptionUtils.makeSalt();

        // 비밀번호 저장
        user.setSalt(salt);
        user.setPassword(passwordEncoder.encode(req.getPassword() + salt));

        userRepository.save(user);

        return FindPasswordDto.Response.builder()
                .msg("비밀번호의 변경이 완료되었습니다. 다시 로그인해주세요.")
                .success(true)
                .build();

    }

    @Override
    public ProfileDto.Response findProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

//        Account account = accountRepository.findById(user.getMainAccount())
//                .orElseThrow(() -> new NotFoundException("계좌를 찾을 수 없습니다."));

        return ProfileDto.Response.builder()
                .msg(user.getName() + "님의 프로필을 불러왔습니다.")
                .success(true)
                .mine(false)
                .imageNumber(user.getImageNumber())
                .name(user.getName())
                .introduce(user.getIntroduce())
                .birth(user.getBirth())
//                .bankCode(account.getBankCode())
//                .account(account.getAccountNumber())
                .build();
    }

    @Override
    public ProfileDto.Response myProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

//        Account account = accountRepository.findById(user.getMainAccount())
//                .orElseThrow(() -> new NotFoundException("계좌를 찾을 수 없습니다."));

        return ProfileDto.Response.builder()
                .msg("내 프로필을 불러왔습니다.")
                .success(true)
                .mine(true)
                .imageNumber(user.getImageNumber())
                .name(user.getName())
                .phoneNumber(user.getPhoneNumber())
                .introduce(user.getIntroduce())
//                .bankCode(account.getBankCode())
//                .account(account.getAccountNumber())
                .build();
    }

    @Override
    public FindUserDto.Response findUser(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        return FindUserDto.Response.builder()
                .msg("회원정보를 불러왔습니다.")
                .success(true)
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .NickName(user.getNickName())
                .imageNumber(user.getImageNumber())
                .build();
    }

    @Override
    public UpdateUserDto.Response updateUser(UpdateUserDto.Request req, Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("유저를 찾을 수 없습니다."));

        // 비밀번호 체크
        if(!passwordEncoder.matches(req.getPassword() + user.getSalt() , user.getPassword())){
            log.info("비밀번호 변경");
            String salt = encryptionUtils.makeSalt();
            String password = passwordEncoder.encode(req.getPassword() + salt);
            user.setSalt(salt);
            user.setPassword(password);
        }

        // 닉네임 체크
        if(!user.getNickName().equals(req.getNickName())){
            log.info("닉네임변경");
            user.setNickName(req.getNickName());
        }

        // 이미지 번호
        if(user.getImageNumber() == null || !user.getImageNumber().equals(req.getImageNumber())){
            log.info("이미지 변경");
            user.setImageNumber(req.getImageNumber());
        }

        userRepository.save(user);
        log.info("완료");
        return UpdateUserDto.Response.builder()
                .msg("회원정보 변경이 완료되었습니다.")
                .success(true)
                .build();
    }



}
