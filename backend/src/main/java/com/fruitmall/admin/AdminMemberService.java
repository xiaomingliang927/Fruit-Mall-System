package com.fruitmall.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.BizException;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");
    /** 临期阈值：到期前 7 天内视为临期 */
    private static final int EXPIRING_DAYS = 7;

    private final UserMapper userMapper;
    private final AdminMemberMapper memberMapper;

    public record MemberVO(Long id, String nickname, String phone, String avatar, Integer level,
                           Integer status, String createdAt, Long orderCount, Long totalGmv,
                           String memberExpireAt, String memberStatus) {
    }

    public record AddMemberRequest(String phone, String nickname, Integer level, Integer durationDays) {
    }

    /** 会员分页列表：关键词 + 是否有消费 + 会员卡状态筛选；订单/消费额整页批量聚合 */
    public PageResult<MemberVO> page(String keyword, Boolean hasOrdered, String memberStatus,
                                     long page, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getNickname, keyword)
                        .or().like(User::getPhone, keyword));
        if (Boolean.TRUE.equals(hasOrdered)) {
            wrapper.exists("SELECT 1 FROM `order` o WHERE o.user_id = user.id AND o.status IN (20, 30, 40)");
        } else if (Boolean.FALSE.equals(hasOrdered)) {
            wrapper.notExists("SELECT 1 FROM `order` o WHERE o.user_id = user.id AND o.status IN (20, 30, 40)");
        }
        switch (memberStatus == null ? "" : memberStatus) {
            case "NONE" -> wrapper.isNull(User::getMemberExpireAt);
            case "EXPIRED" -> wrapper.apply("member_expire_at < NOW()");
            case "EXPIRING" -> wrapper.apply("member_expire_at >= NOW() AND member_expire_at < DATE_ADD(NOW(), INTERVAL " + EXPIRING_DAYS + " DAY)");
            case "ACTIVE" -> wrapper.apply("member_expire_at >= DATE_ADD(NOW(), INTERVAL " + EXPIRING_DAYS + " DAY)");
            default -> { }
        }
        wrapper.orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<User> users = result.getRecords();
        Map<Long, Map<String, Object>> aggByUser = users.isEmpty() ? Map.of()
                : memberMapper.orderAggByUsers(users.stream().map(User::getId).toList()).stream()
                        .collect(Collectors.toMap(m -> ((Number) m.get("userId")).longValue(), Function.identity()));
        return PageResult.of(result, u -> toVO(u, aggByUser.getOrDefault(u.getId(), Map.of())));
    }

    /**
     * 开卡 / 续费（模型 B）：
     * 手机号不存在 → 新建会员，有效期 = 现在 + durationDays；
     * 已存在 → 续费，有效期 = max(现在, 原到期日) + durationDays（未到期续费自动顺延）。
     */
    @Transactional
    public MemberVO addMember(AddMemberRequest req) {
        if (req.phone() == null || !PHONE.matcher(req.phone()).matches()) {
            throw new BizException("请输入正确的 11 位手机号");
        }
        Integer level = req.level() == null ? 1 : req.level();
        if (level < 1 || level > 4) {
            throw new BizException("会员等级不合法");
        }
        if (req.durationDays() == null || req.durationDays() < 1 || req.durationDays() > 3650) {
            throw new BizException("有效期须在 1~3650 天之间");
        }
        LocalDateTime now = LocalDateTime.now();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, req.phone()));
        boolean renewed = user != null;
        if (user == null) {
            user = new User();
            user.setPhone(req.phone());
            user.setNickname(StringUtils.hasText(req.nickname())
                    ? req.nickname() : "会员" + req.phone().substring(req.phone().length() - 4));
            user.setStatus(1);
        } else {
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BizException("该账号已被禁用，无法开卡");
            }
            if (StringUtils.hasText(req.nickname())) {
                user.setNickname(req.nickname());
            }
        }
        user.setLevel(level);
        LocalDateTime base = !renewed || user.getMemberExpireAt() == null
                || user.getMemberExpireAt().isBefore(now)
                ? now : user.getMemberExpireAt();
        user.setMemberExpireAt(base.plusDays(req.durationDays()));
        if (renewed) {
            userMapper.updateById(user);
        } else {
            userMapper.insert(user);
        }
        Map<String, Object> agg = memberMapper.orderAggByUsers(List.of(user.getId()))
                .stream().findFirst().orElse(Map.of());
        return toVO(user, agg);
    }

    /** 会员卡状态：NONE 未开通 / ACTIVE 正常 / EXPIRING 临期(7天内到期) / EXPIRED 已过期 */
    public static String memberStatusOf(LocalDateTime expire) {
        if (expire == null) {
            return "NONE";
        }
        LocalDateTime now = LocalDateTime.now();
        if (expire.isBefore(now)) {
            return "EXPIRED";
        }
        return expire.isBefore(now.plusDays(EXPIRING_DAYS)) ? "EXPIRING" : "ACTIVE";
    }

    private MemberVO toVO(User u, Map<String, Object> agg) {
        LocalDateTime expire = u.getMemberExpireAt();
        return new MemberVO(u.getId(), u.getNickname(), maskPhone(u.getPhone()), u.getAvatar(),
                u.getLevel(), u.getStatus(),
                u.getCreatedAt() == null ? null : u.getCreatedAt().toLocalDate().toString(),
                agg.isEmpty() ? 0L : ((Number) agg.get("orderCount")).longValue(),
                agg.isEmpty() ? 0L : ((Number) agg.get("totalGmv")).longValue(),
                expire == null ? null : expire.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                memberStatusOf(expire));
    }

    private static String maskPhone(String phone) {
        return phone == null || phone.length() < 7 ? phone
                : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
