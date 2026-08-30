package com.fruitmall.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.user.User;
import com.fruitmall.modules.user.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final UserMapper userMapper;
    private final AdminMemberMapper memberMapper;

    public record MemberVO(Long id, String nickname, String phone, String avatar, Integer level,
                           Integer status, String createdAt, Long orderCount, Long totalGmv) {
    }

    /** 会员分页列表：关键词（昵称/手机号）+ 是否有消费筛选；订单数/消费额整页批量聚合避免 N+1 */
    public PageResult<MemberVO> page(String keyword, Boolean hasOrdered, long page, long size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getNickname, keyword)
                        .or().like(User::getPhone, keyword));
        if (Boolean.TRUE.equals(hasOrdered)) {
            wrapper.exists("SELECT 1 FROM `order` o WHERE o.user_id = user.id AND o.status IN (20, 30, 40)");
        } else if (Boolean.FALSE.equals(hasOrdered)) {
            wrapper.notExists("SELECT 1 FROM `order` o WHERE o.user_id = user.id AND o.status IN (20, 30, 40)");
        }
        wrapper.orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<User> users = result.getRecords();
        Map<Long, Map<String, Object>> aggByUser = users.isEmpty() ? Map.of()
                : memberMapper.orderAggByUsers(users.stream().map(User::getId).toList()).stream()
                        .collect(Collectors.toMap(m -> ((Number) m.get("userId")).longValue(), Function.identity()));
        return PageResult.of(result, u -> toVO(u, aggByUser.getOrDefault(u.getId(), Map.of())));
    }

    private MemberVO toVO(User u, Map<String, Object> agg) {
        return new MemberVO(u.getId(), u.getNickname(), maskPhone(u.getPhone()), u.getAvatar(),
                u.getLevel(), u.getStatus(),
                u.getCreatedAt() == null ? null : u.getCreatedAt().toLocalDate().toString(),
                agg.isEmpty() ? 0L : ((Number) agg.get("orderCount")).longValue(),
                agg.isEmpty() ? 0L : ((Number) agg.get("totalGmv")).longValue());
    }

    private static String maskPhone(String phone) {
        return phone == null || phone.length() < 7 ? phone
                : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
