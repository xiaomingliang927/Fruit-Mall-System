package com.fruitmall.modules.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.admin.AdminMemberService;
import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.BizException;
import com.fruitmall.common.HtmlSanitizer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserAddressMapper addressMapper;
    private final MemberStatMapper memberStatMapper;

    public record UserVO(Long id, String phone, String nickname, String avatar, Integer level) {
        static UserVO of(User u) {
            return new UserVO(u.getId(), maskPhone(u.getPhone()), u.getNickname(), u.getAvatar(), u.getLevel());
        }

        private static String maskPhone(String phone) {
            return phone == null || phone.length() < 7 ? phone
                    : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        }
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        User user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return ApiResponse.ok(UserVO.of(user));
    }

    /**
     * 会员卡概览（模型 B 限期会员卡）。
     * status: NONE 未开通 / ACTIVE 生效中 / EXPIRING 7 天内到期 / EXPIRED 已过期；
     * remainDays 仅在 ACTIVE、EXPIRING 时有意义；totalGmv 单位为分。
     */
    public record MemberOverviewVO(Integer level, String memberExpireAt, String memberStatus,
                                   Long remainDays, Long orderCount, Long totalGmv,
                                   String joinedAt, Long joinedDays) {
    }

    @GetMapping("/me/member")
    public ApiResponse<MemberOverviewVO> myMember() {
        User user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        LocalDateTime expire = user.getMemberExpireAt();
        LocalDateTime now = LocalDateTime.now();
        Long remainDays = expire == null || expire.isBefore(now)
                ? null : ChronoUnit.DAYS.between(now, expire);
        Map<String, Object> agg = memberStatMapper.orderAggByUser(user.getId());
        LocalDateTime createdAt = user.getCreatedAt();
        return ApiResponse.ok(new MemberOverviewVO(
                user.getLevel(),
                expire == null ? null : expire.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                AdminMemberService.memberStatusOf(expire),
                remainDays,
                agg == null ? 0L : ((Number) agg.getOrDefault("orderCount", 0)).longValue(),
                agg == null ? 0L : ((Number) agg.getOrDefault("totalGmv", 0)).longValue(),
                createdAt == null ? null : createdAt.toLocalDate().toString(),
                createdAt == null ? 0L : ChronoUnit.DAYS.between(createdAt.toLocalDate(), now.toLocalDate())));
    }

    @GetMapping("/me/addresses")
    public ApiResponse<List<UserAddress>> myAddresses() {
        return ApiResponse.ok(addressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, UserContext.requireUserId())
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId)));
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "昵称不能为空") String nickname,
            String avatar
    ) {
    }

    @PutMapping("/me")
    public ApiResponse<UserVO> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        User user = userMapper.selectById(UserContext.requireUserId());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setNickname(HtmlSanitizer.sanitize(request.nickname()));
        if (request.avatar() != null) {
            user.setAvatar(HtmlSanitizer.sanitize(request.avatar()));
        }
        userMapper.updateById(user);
        return ApiResponse.ok(UserVO.of(user));
    }

    public record AddAddressRequest(
            @NotBlank(message = "收货人不能为空") String receiver,
            @NotBlank(message = "手机号不能为空") String phone,
            @NotBlank(message = "省份不能为空") String province,
            @NotBlank(message = "城市不能为空") String city,
            String district,
            @NotBlank(message = "详细地址不能为空") String detail,
            Boolean isDefault
    ) {
    }

    @PostMapping("/me/addresses")
    public ApiResponse<Long> addAddress(@Valid @RequestBody AddAddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUserId(UserContext.requireUserId());
        address.setReceiver(HtmlSanitizer.sanitize(request.receiver()));
        address.setPhone(HtmlSanitizer.sanitize(request.phone()));
        address.setProvince(HtmlSanitizer.sanitize(request.province()));
        address.setCity(HtmlSanitizer.sanitize(request.city()));
        address.setDistrict(HtmlSanitizer.sanitize(request.district()));
        address.setDetail(HtmlSanitizer.sanitize(request.detail()));
        address.setIsDefault(Boolean.TRUE.equals(request.isDefault()));
        addressMapper.insert(address);
        return ApiResponse.ok(address.getId());
    }

    @DeleteMapping("/me/addresses/{id}")
    public ApiResponse<Void> deleteAddress(@PathVariable Long id) {
        UserAddress address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(UserContext.requireUserId())) {
            throw new BizException("地址不存在");
        }
        addressMapper.deleteById(id);
        return ApiResponse.ok();
    }
}
