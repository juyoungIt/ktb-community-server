package com.ktb.howard.ktb_community_server.member.service;

import com.ktb.howard.ktb_community_server.image.service.ImageService;
import com.ktb.howard.ktb_community_server.member.domain.Member;
import com.ktb.howard.ktb_community_server.member.dto.MemberCreateRequestDto;
import com.ktb.howard.ktb_community_server.member.exception.*;
import com.ktb.howard.ktb_community_server.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private ImageService imageService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("회원가입 단위 테스트")
    class CreatMemberTest {

        @Test
        @DisplayName("회원가입 성공 (프로필 이미지X) - 중복되지 않은 이메일, 닉네임으로 회원가입에 성공한다.")
        void createdMemberSuccessNoProfileImageTest() {
            // given
            MemberCreateRequestDto request = MemberCreateRequestDto.builder()
                    .email("test-email@kakaotech.com")
                    .password("test-password-12345")
                    .nickname("test-nickname")
                    .profileImageId(null)
                    .build();
            given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
            given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
                Member arg = invocation.getArgument(0);
                ReflectionTestUtils.setField(arg, "id", 1); // DB가 ID값을 주입하는 행위 재현
                return arg;
            });

            // when
            Member createdMember = memberService.createMember(request);

            // then
            assertThat(createdMember.getEmail()).isEqualTo(request.getEmail());
            assertThat(createdMember.getPassword()).isEqualTo("encodedPassword"); // 암호화된 비밀번호
            assertThat(createdMember.getNickname()).isEqualTo(request.getNickname());
            then(memberRepository).should(times(1)).save(any(Member.class));
            then(memberRepository).should(times(1)).existsByEmail(request.getEmail());
            then(memberRepository).should(times(1)).existsByNickname(request.getNickname());
            then(passwordEncoder).should(times(1)).encode(request.getPassword());
        }

        @Test
        @DisplayName("회원가입 성공 (프로필 이미지O) - 중복되지 않은 이메일, 닉네임으로 회원가입에 성공한다.")
        void createdMemberSuccessWithProfileImageTest() {
            // given
            MemberCreateRequestDto request = MemberCreateRequestDto.builder()
                    .email("test-email@kakaotech.com")
                    .password("test-password-12345")
                    .nickname("test-nickname")
                    .profileImageId(1L)
                    .build();
            given(memberRepository.existsByEmail(request.getEmail())).willReturn(false);
            given(memberRepository.existsByNickname(request.getNickname())).willReturn(false);
            given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");
            given(imageService.isExist(request.getProfileImageId())).willReturn(true);
            given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
                Member arg = invocation.getArgument(0);
                ReflectionTestUtils.setField(arg, "id", 1); // DB가 ID값을 주입하는 행위 재현
                return arg;
            });

            // when
            Member createdMember = memberService.createMember(request);

            // then
            assertThat(createdMember.getEmail()).isEqualTo(request.getEmail());
            assertThat(createdMember.getPassword()).isEqualTo("encodedPassword"); // 암호화된 비밀번호
            assertThat(createdMember.getNickname()).isEqualTo(request.getNickname());
            then(memberRepository).should(times(1)).save(any(Member.class));
            then(memberRepository).should(times(1)).existsByEmail(request.getEmail());
            then(memberRepository).should(times(1)).existsByNickname(request.getNickname());
            then(passwordEncoder).should(times(1)).encode(request.getPassword());
            then(imageService).should(times(1)).isExist(request.getProfileImageId());
            then(imageService).should(times(1))
                    .persistImage(eq(request.getProfileImageId()), any(Member.class), eq(1L), eq(1));
        }

        @Test
        @DisplayName("회원가입 실패 - 이미 존재하는 이메일에 대해 가입을 시도하는 경우, 예외를 발생시킨다")
        void createdMemberFailedWhenDuplicatedEmailTest() {
            // given
            MemberCreateRequestDto request = MemberCreateRequestDto.builder()
                    .email("test-email@kakaotech.com")
                    .password("test-password-12345")
                    .nickname("test-nickname")
                    .profileImageId(null)
                    .build();
            given(memberRepository.existsByEmail(request.getEmail())).willReturn(true);

            // when // then
            assertThatThrownBy(() -> memberService.createMember(request))
                    .isInstanceOf(AlreadyUsedEmailException.class);
        }

        @Test
        @DisplayName("회원가입 실패 - 이미 존재하는 닉네임 대해 가입을 시도하는 경우, 예외를 발생시킨다")
        void createdMemberFailedWhenDuplicatedNicknameTest() {
            // given
            MemberCreateRequestDto request = MemberCreateRequestDto.builder()
                    .email("test-email@kakaotech.com")
                    .password("test-password-12345")
                    .nickname("test-nickname")
                    .profileImageId(null)
                    .build();
            given(memberRepository.existsByNickname(request.getNickname())).willReturn(true);

            // when // then
            assertThatThrownBy(() -> memberService.createMember(request))
                    .isInstanceOf(AlreadyUsedNicknameException.class);
        }

        @Test
        @DisplayName("회원가입 실패 - 존재하지 않는 이미지를 프로필로 사용하려는 경우, 예외를 발생시킨다")
        void createdMemberFailedWhenNotExistProfileImageTest() {
            // given
            MemberCreateRequestDto request = MemberCreateRequestDto.builder()
                    .email("test-email@kakaotech.com")
                    .password("test-password-12345")
                    .nickname("test-nickname")
                    .profileImageId(1L)
                    .build();
            given(imageService.isExist(any(Long.class))).willReturn(false);

            // when // then
            assertThatThrownBy(() -> memberService.createMember(request))
                    .isInstanceOf(ProfileImageNotFoundException.class);
        }

    }

}