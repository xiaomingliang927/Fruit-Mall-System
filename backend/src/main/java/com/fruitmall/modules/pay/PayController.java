package com.fruitmall.modules.pay;

import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PayController {

    private final PayService payService;

    /** 开发模式模拟支付成功（M2 替换为真实的微信支付 prepay + notify 回调） */
    @PostMapping("/{orderNo}/mock-pay")
    public ApiResponse<Void> mockPay(@PathVariable String orderNo) {
        payService.mockPay(UserContext.requireUserId(), orderNo);
        return ApiResponse.ok();
    }
}
