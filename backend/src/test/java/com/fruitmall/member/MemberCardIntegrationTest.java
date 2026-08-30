package com.fruitmall.member;

import com.fruitmall.admin.AdminMemberService;
import com.fruitmall.common.BizException;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** 会员卡（模型 B）开卡/续费集成测试 */
@SpringBootTest
@ActiveProfiles("test")
class MemberCardIntegrationTest {

    @Autowired AdminMemberService memberService;
    @Autowired UserMapper userMapper;

    private String phone() {
        return "137" + String.valueOf(System.nanoTime()).substring(0, 8);
    }

    @Test
    @DisplayName("开卡：手机号不存在则新建会员，有效期 = 现在 + 天数")
    void addMember_createsUserWithExpire() {
        String p = phone();
        AdminMemberService.MemberVO vo = memberService.addMember(
                new AdminMemberService.AddMemberRequest(p, null, 3, 365));

        assertThat(vo.memberStatus()).isEqualTo("ACTIVE");
        assertThat(vo.memberExpireAt()).isNotBlank();

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, p));
        assertThat(user.getMemberExpireAt()).isCloseTo(LocalDateTime.now().plusDays(365), within(60L, java.time.temporal.ChronoUnit.SECONDS));
        assertThat(user.getLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("续费：未到期续费在原到期日上顺延，而不是从现在叠加")
    void addMember_renewsFromOriginalExpire() {
        String p = phone();
        memberService.addMember(new AdminMemberService.AddMemberRequest(p, null, 1, 100));
        User first = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, p));
        LocalDateTime firstExpire = first.getMemberExpireAt();

        memberService.addMember(new AdminMemberService.AddMemberRequest(p, "续费用户", 1, 30));
        User renewed = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, p));

        assertThat(renewed.getMemberExpireAt()).isCloseTo(firstExpire.plusDays(30), within(60L, java.time.temporal.ChronoUnit.SECONDS));
        assertThat(renewed.getNickname()).isEqualTo("续费用户");
    }

    @Test
    @DisplayName("过期后续费从现在起算，而不是在过期日上叠加")
    void addMember_renewsFromNowWhenExpired() {
        String p = phone();
        // 手工造一个已过期会员
        User expired = new User();
        expired.setPhone(p);
        expired.setNickname("过期会员");
        expired.setLevel(1);
        expired.setStatus(1);
        expired.setMemberExpireAt(LocalDateTime.now().minusDays(10));
        userMapper.insert(expired);

        memberService.addMember(new AdminMemberService.AddMemberRequest(p, null, 1, 90));
        User renewed = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, p));

        assertThat(renewed.getMemberExpireAt()).isCloseTo(LocalDateTime.now().plusDays(90), within(60L, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("校验：非法手机号 / 非法天数被拒绝")
    void addMember_validates() {
        assertThatThrownBy(() -> memberService.addMember(
                new AdminMemberService.AddMemberRequest("12345", null, 1, 365)))
                .isInstanceOf(BizException.class);
        assertThatThrownBy(() -> memberService.addMember(
                new AdminMemberService.AddMemberRequest(phone(), null, 1, 0)))
                .isInstanceOf(BizException.class);
    }
}
