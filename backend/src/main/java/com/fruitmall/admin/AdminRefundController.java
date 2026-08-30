package com.fruitmall.admin;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import com.fruitmall.modules.refund.Refund;
import com.fruitmall.modules.refund.RefundService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    @GetMapping
    public ApiResponse<PageResult<RefundService.AdminRefundVO>> page(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(refundService.adminPage(status, keyword, page, Math.min(size, 50)));
    }

    public record AuditRequest(String remark) {
    }

    /** 审核通过：完成退款（模拟打款），订单转已退款并回补库存 */
    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id,
                                     @RequestBody(required = false) AuditRequest request) {
        AdminContext.requireAdminId();
        refundService.adminApprove(AdminContext.getAdminId(), id,
                request == null ? null : request.remark());
        return ApiResponse.ok();
    }

    /** 审核驳回：订单恢复申请前状态 */
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id,
                                    @Valid @RequestBody RejectRequest request) {
        AdminContext.requireAdminId();
        refundService.adminReject(AdminContext.getAdminId(), id, request.remark());
        return ApiResponse.ok();
    }

    public record RejectRequest(@NotBlank(message = "请填写驳回原因") String remark) {
    }
}
