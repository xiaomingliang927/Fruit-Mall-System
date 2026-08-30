package com.fruitmall.admin;

import com.fruitmall.common.ApiResponse;
import com.fruitmall.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public ApiResponse<PageResult<AdminMemberService.MemberVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean hasOrdered,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(adminMemberService.page(keyword, hasOrdered, page, Math.min(size, 50)));
    }
}
