package com.ktb.howard.ktb_community_server.member.service;

import com.ktb.howard.ktb_community_server.image.dto.ImageUrlResponseDto;
import com.ktb.howard.ktb_community_server.image.service.ImageService;
import com.ktb.howard.ktb_community_server.member.domain.Member;
import com.ktb.howard.ktb_community_server.member.dto.MemberCreateRequestDto;
import com.ktb.howard.ktb_community_server.member.dto.MemberInfoResponseDto;
import com.ktb.howard.ktb_community_server.member.exception.*;
import com.ktb.howard.ktb_community_server.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ktb.howard.ktb_community_server.api.MemberErrorCode.*;
import static com.ktb.howard.ktb_community_server.image.domain.ImageType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Spy
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

    @Nested
    @DisplayName("이메일 중복확인 단위 테스트")
    class checkEmailTest {

        @Test
        @DisplayName("이메일 체크 (중복X) - 사용된 적 없는 이메일을 입력한 경우, 예외발생 없이 호출이 종료된다")
        void checkEmailSuccess() {
            // given
            String requestEmail = "test@kakaotech.com";
            given(memberRepository.existsByEmail(requestEmail)).willReturn(false);

            // when // then
            assertThatCode(() -> memberService.checkEmail(requestEmail)).doesNotThrowAnyException();
            then(memberRepository).should(times(1)).existsByEmail(requestEmail);
        }

        @Test
        @DisplayName("이메일 체크 (중복O) - 사용된 적 있는 이메일을 입력한 경우, 예외를 발생시킨다")
        void checkEmailFailed() {
            // given
            String requestEmail = "test@kakaotech.com";
            given(memberRepository.existsByEmail(requestEmail)).willReturn(true);

            // when // then
            assertThatThrownBy(() -> memberService.checkEmail(requestEmail))
                    .isInstanceOf(AlreadyUsedEmailException.class)
                    .hasMessage(ALREADY_USED_EMAIL.getMessage());
            then(memberRepository).should(times(1)).existsByEmail(requestEmail);
        }

    }

    @Nested
    @DisplayName("닉네임 중복확인 단위 테스트")
    class checkNicknameTest {

        @Test
        @DisplayName("닉네임 체크 (중복X) - 사용된 적 없는 닉네임을 입력한 경우, 예외발생 없이 호출이 종료된다")
        void checkNicknameSuccess() {
            // given
            String requestNickname = "testNickname";
            given(memberRepository.existsByNickname(requestNickname)).willReturn(false);

            // when // then
            assertThatCode(() -> memberService.checkNickname(requestNickname)).doesNotThrowAnyException();
            then(memberRepository).should(times(1)).existsByNickname(requestNickname);
        }

        @Test
        @DisplayName("닉네임 체크 (중복O) - 사용된 적 있는 닉네임을 입력한 경우, 예외를 발생시킨다")
        void checkNicknameFailed() {
            // given
            String requestNickname = "testNickname";
            given(memberRepository.existsByNickname(requestNickname)).willReturn(true);

            // when // then
            assertThatThrownBy(() -> memberService.checkNickname(requestNickname))
                    .isInstanceOf(AlreadyUsedNicknameException.class)
                    .hasMessage(ALREADY_USED_NICKNAME.getMessage());
            then(memberRepository).should(times(1)).existsByNickname(requestNickname);
        }

    }

    @Nested
    @DisplayName("회원정보 조회 단위 테스트")
    class getProfileTest {

        @Test
        @DisplayName("회원정보 조회 - 요청 ID에 매핑되는 회원정보를 프로필 이미지와 함께 반환한다")
        void getProfileSuccess() {
            // given
            Integer requestMemberId = 1;
            Long generatedImageId = 1L;
            Member member = Member.builder()
                    .email("test@kakaotech.com")
                    .password("testPassword12345!")
                    .nickname("testNickname")
                    .build();
            ReflectionTestUtils.setField(member, "id", requestMemberId);
            ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(member, "deletedAt", null);
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.of(member));
            ImageUrlResponseDto imageUrlResponseDto = new ImageUrlResponseDto(
                    "https://test-s3-bucket.com/test-image",
                    generatedImageId,
                    1,
                    "GET",
                    Instant.now()
            );
            given(imageService.createImageViewUrl(any()))
                    .willReturn(List.of(imageUrlResponseDto));

            // when
            MemberInfoResponseDto profile = memberService.getProfile(requestMemberId);

            // then
            assertThat(profile)
                    .extracting("email", "nickname", "imageId", "profileImageUrl")
                    .containsExactly(
                            member.getEmail(),
                            member.getNickname(),
                            imageUrlResponseDto.imageId(),
                            imageUrlResponseDto.url()
                    );
            then(memberRepository).should(times(1)).findById(requestMemberId.longValue());
            then(imageService).should(times(1)).createImageViewUrl(any());
        }

        @Test
        @DisplayName("회원정보 조회 - 요청 ID에 매핑되는 회원정보가 탈퇴회원인 경우, 마스킹 처리 후 회원정보를 반환한다")
        void getProfileSuccessWhenResignedMember() {
            // given
            Integer requestMemberId = 1;
            Long generatedImageId = 1L;
            Member member = Member.builder()
                    .email("test@kakaotech.com")
                    .password("testPassword12345!")
                    .nickname("testNickname")
                    .build();
            ReflectionTestUtils.setField(member, "id", requestMemberId);
            ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(member, "deletedAt", LocalDateTime.now());
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.of(member));
            ImageUrlResponseDto imageUrlResponseDto = new ImageUrlResponseDto(
                    "https://test-s3-bucket.com/test-image",
                    generatedImageId,
                    1,
                    "GET",
                    Instant.now()
            );

            // when
            MemberInfoResponseDto profile = memberService.getProfile(requestMemberId);

            // then
            assertThat(profile.getEmail()).isNull();
            assertThat(profile.getNickname()).isEqualTo("탈퇴한 회원");
            assertThat(profile.getImageId()).isNull();
            assertThat(profile.getProfileImageUrl()).isNull();
            then(memberRepository).should(times(1)).findById(requestMemberId.longValue());
            then(imageService).should(times(0)).createImageViewUrl(any());
        }

        @Test
        @DisplayName("회원정보 조회 - 요청 ID에 매핑되는 회원정보가 없는 경우, 예외를 발생시킨다")
        void getProfileFailed() {
            // given
            Integer requestMemberId = 1;
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.empty());

            // when // then
            assertThatThrownBy(() -> memberService.getProfile(requestMemberId))
                    .isInstanceOf(MemberNotFoundException.class)
                    .hasMessage(MEMBER_NOT_FOUND.getMessage());
        }

    }

    @Nested
    @DisplayName("회원정보 수정 단위 테스트")
    class updateMemberTest {

        @Test
        @DisplayName("회원정보(닉네임) 수정 성공 - 사용되지 않은 닉네임을 입력한 경우, 변경에 성공한다")
        void updateMemberSuccessWhenValidNickname() {
            // given
            Integer requestMemberId = 1;
            Member member = Member.builder()
                    .email("test@kakaotech.com")
                    .password("testPassword12345!")
                    .nickname("testNickname")
                    .build();
            String newNickname = "newNickname";
            ReflectionTestUtils.setField(member, "id", requestMemberId);
            ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(member, "deletedAt", null);
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.of(member));
            willDoNothing().given(memberService).checkNickname(newNickname);

            // when
            memberService.updateMember(
                    requestMemberId,
                    newNickname,
                    null,
                    null,
                    null,
                    null
            );

            // then
            assertThat(member.getNickname()).isEqualTo(newNickname);
            then(memberRepository).should(times(1)).findById(requestMemberId.longValue());
            then(memberService).should(times(1)).checkNickname(newNickname);
            then(passwordEncoder).shouldHaveNoInteractions();
            then(imageService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원정보(닉네임) 수정 실패 - 이미 사용된 닉네임으로 변경을 시도하는 경우, 예외를 발생시킨다")
        void updateMemberFailedWhenInvalidNickname() {
            Integer requestMemberId = 1;
            Member member = Member.builder()
                    .email("test@kakaotech.com")
                    .password("testPassword12345!")
                    .nickname("testNickname")
                    .build();
            String newNickname = "newNickname";
            ReflectionTestUtils.setField(member, "id", requestMemberId);
            ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(member, "deletedAt", null);
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.of(member));
            willThrow(new AlreadyUsedNicknameException(ALREADY_USED_NICKNAME))
                    .given(memberService).checkNickname(newNickname);

            // when // then
            assertThatThrownBy(() -> memberService.updateMember(
                    requestMemberId,
                    newNickname,
                    null,
                    null,
                    null,
                    null
                )
            ).isInstanceOf(AlreadyUsedNicknameException.class)
                    .hasMessage(ALREADY_USED_NICKNAME.getMessage());
            then(memberRepository).should(times(1)).findById(requestMemberId.longValue());
            then(memberService).should(times(1)).checkNickname(newNickname);
            then(passwordEncoder).shouldHaveNoInteractions();
            then(imageService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원정보(비밀번호) 수정 성공 - 정책을 준수한 올바른 비밀번호를 입력한 경우, 변경에 성공한다")
        void updateMemberSuccessWhenValidPassword() {
            // given
            Integer requestMemberId = 1;
            String curPassword = "testPassword12345!";
            String newPassword = "newPassword12345!";
            Member member = Member.builder()
                    .email("test@kakaotech.com")
                    .password(curPassword)
                    .nickname("testNickname")
                    .build();
            ReflectionTestUtils.setField(member, "id", requestMemberId);
            ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(member, "deletedAt", null);
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.of(member));
            given(passwordEncoder.matches(curPassword, curPassword)).willReturn(true);
            given(passwordEncoder.encode(newPassword)).willReturn(newPassword);

            // when
            memberService.updateMember(
                    requestMemberId,
                    null,
                    curPassword,
                    newPassword,
                    null,
                    null
            );

            // then
            assertThat(member.getPassword()).isEqualTo(newPassword);
            then(passwordEncoder).should(times(1)).matches(curPassword, curPassword);
            then(passwordEncoder).should(times(1)).encode(newPassword);
            then(imageService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원정보(비밀번호) 수정 성공 - 정책을 준수한 올바른 비밀번호를 입력한 경우, 변경에 성공한다")
        void updateMemberFailedWhenInvalidPassword() {
            // given
            Integer requestMemberId = 1;
            String curPassword = "testPassword12345!";
            String newPassword = "newPassword12345!";
            Member member = Member.builder()
                    .email("test@kakaotech.com")
                    .password(curPassword)
                    .nickname("testNickname")
                    .build();
            ReflectionTestUtils.setField(member, "id", requestMemberId);
            ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(member, "deletedAt", null);
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.of(member));
            given(passwordEncoder.matches(curPassword, curPassword)).willReturn(false);

            // when // then
            assertThatThrownBy(() -> memberService.updateMember(
                    requestMemberId,
                    null,
                    curPassword,
                    newPassword,
                    null,
                    null
            )).isInstanceOf(PasswordNotMatchedException.class)
                    .hasMessage(PASSWORD_NOT_MATCHED.getMessage());
            then(passwordEncoder).should(times(1)).matches(curPassword, curPassword);
            then(imageService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원정보(프로필 이미지) 제거 성공 - 기존 프로필 이미지에 대한 제거 요청이 들어온 경우, 프로필 이미지를 제거한다")
        void updateMemberSuccessWhenDeleteProfileImage() {
            // given
            Integer requestMemberId = 1;
            Member member = Member.builder()
                    .email("test@kakaotech.com")
                    .password("testPassword12345!")
                    .nickname("testNickname")
                    .build();
            ReflectionTestUtils.setField(member, "id", requestMemberId);
            ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(member, "deletedAt", null);
            Boolean deleteProfileImage = true;
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.of(member));
            given(imageService.findImageIds(PROFILE, requestMemberId.longValue())).willReturn(List.of(1L));

            // then
            memberService.updateMember(
                    requestMemberId,
                    null,
                    null,
                    null,
                    null,
                    deleteProfileImage
            );

            // when
            then(imageService).should(times(1))
                    .findImageIds(PROFILE, requestMemberId.longValue());
            then(imageService).should(times(1)).deleteImage(1L);
            then(passwordEncoder).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원정보(프로필 이미지) 추가 성공 - 프로필 이미지 추가에 대한 요청이 들어온 경우, 프로필 이미지를 추가한다")
        void updateMemberSuccessWhenAddProfileImage() {
            // given
            Integer requestMemberId = 1;
            Member member = Member.builder()
                    .email("test@kakaotech.com")
                    .password("testPassword12345!")
                    .nickname("testNickname")
                    .build();
            ReflectionTestUtils.setField(member, "id", requestMemberId);
            ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(member, "deletedAt", null);
            Long profileImageId = 2L;
            Boolean deleteProfileImage = false;
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.of(member));

            // when
            memberService.updateMember(
                    requestMemberId,
                    null,
                    null,
                    null,
                    profileImageId,
                    deleteProfileImage
            );

            // then
            then(imageService).should(times(0)).findImageIds(PROFILE, requestMemberId.longValue());
            then(imageService).should(times(0)).deleteImage(1L);
            then(imageService).should(times(1))
                    .persistImage(profileImageId, member, member.getId().longValue(), 1);
            then(passwordEncoder).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원정보(프로필 이미지) 변경 성공 - 프로필 이미지 추가에 대한 요청이 들어온 경우, 프로필 이미지를 변경한다")
        void updateMemberSuccessWhenChangeProfileImage() {
            // given
            Integer requestMemberId = 1;
            Member member = Member.builder()
                    .email("test@kakaotech.com")
                    .password("testPassword12345!")
                    .nickname("testNickname")
                    .build();
            ReflectionTestUtils.setField(member, "id", requestMemberId);
            ReflectionTestUtils.setField(member, "createdAt", LocalDateTime.now());
            ReflectionTestUtils.setField(member, "deletedAt", null);
            Long profileImageId = 2L;
            Boolean deleteProfileImage = true;
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.of(member));
            given(imageService.findImageIds(PROFILE, requestMemberId.longValue())).willReturn(List.of(1L));

            // when
            memberService.updateMember(
                    requestMemberId,
                    null,
                    null,
                    null,
                    profileImageId,
                    deleteProfileImage
            );

            // then
            then(imageService).should(times(1)).findImageIds(PROFILE, requestMemberId.longValue());
            then(imageService).should(times(1)).deleteImage(1L);
            then(imageService).should(times(1))
                    .persistImage(profileImageId, member, member.getId().longValue(), 1);
            then(passwordEncoder).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원정보 수정 실패 - 존재하지 않는 회원에 대하여 정보 수정을 시도하는 경우, 예외를 발생시킨다")
        void updateMemberFailedWhenMemberNotExist() {
            // given
            Integer requestMemberId = 1;
            given(memberRepository.findById(requestMemberId.longValue())).willReturn(Optional.empty());

            // when // then
            assertThatThrownBy(() -> memberService.updateMember(
                    requestMemberId,
                    "newNickname",
                    null,
                    null,
                    null,
                    null)
            )
                    .isInstanceOf(MemberNotFoundException.class)
                    .hasMessage(MEMBER_NOT_FOUND.getMessage());
            then(memberRepository).should(times(1)).findById(requestMemberId.longValue());
        }

    }

}