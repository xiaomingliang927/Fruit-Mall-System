package com.fruitmall.modules.refund;

import com.fruitmall.auth.UserContext;
import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@RestController
@RequestMapping("/api/v1/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public record ApplyRequest(
            @NotBlank(message = "订单号不能为空") String orderNo,
            @NotNull(message = "请选择售后类型") Integer type,
            @NotBlank(message = "请填写申请原因") String reason,
            List<String> images
    ) {
    }

    /** 申请售后（≤50 元自动秒审退款） */
    @PostMapping
    public ApiResponse<RefundService.RefundVO> apply(@Valid @RequestBody ApplyRequest request) {
                String imagesJson = null;
        try {
            if (request.images() != null && !request.images().isEmpty()) {
                imagesJson = objectMapper.writeValueAsString(request.images());
            }
        } catch (Exception e) { throw new com.fruitmall.common.BizException("凭证图参数不合法"); }
        return ApiResponse.ok(refundService.apply(
                UserContext.requireUserId(), request.orderNo(), request.type(), request.reason(), imagesJson));
    }

    /** 我的售后单 */
    @GetMapping
    public ApiResponse<PageResult<RefundService.RefundVO>> myRefunds(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(refundService.myRefunds(UserContext.requireUserId(), page, Math.min(size, 50)));
    }

    /** 售后单详情 */
    @GetMapping("/{refundNo}")
    public ApiResponse<RefundService.RefundVO> detail(@PathVariable String refundNo) {
        return ApiResponse.ok(refundService.detail(UserContext.requireUserId(), refundNo));
    }

    /** 撤销待审核的售后申请（订单恢复原状态） */
    @PostMapping("/{refundNo}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable String refundNo) {
        refundService.withdraw(UserContext.requireUserId(), refundNo);
        return ApiResponse.ok();
    }
}
