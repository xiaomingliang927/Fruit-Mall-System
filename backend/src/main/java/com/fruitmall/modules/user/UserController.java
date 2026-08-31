package com.fruitmall.modules.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.BizException;
import com.fruitmall.common.HtmlSanitizer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final UserAddressMapper addressMapper;

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

    @GetMapping("/me/addresses")
    public ApiResponse<List<UserAddress>> myAddresses() {
        return ApiResponse.ok(addressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, UserContext.requireUserId())
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId)));
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
