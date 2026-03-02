package com.idstory.history.controller;

import com.idstory.history.entity.UserAccountHistory;
import com.idstory.history.service.UserAccountHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * 사용자 계정 이력 조회 컨트롤러
 */
@Controller
@RequestMapping("/history")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserHistoryController {

    private static final int PAGE_SIZE = 20;

    private final UserAccountHistoryService historyService;

    /**
     * 사용자 계정 이력 목록
     * GET /history/user-account
     */
    @GetMapping("/user-account")
    public String userAccount(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        Page<UserAccountHistory> historyPage =
                historyService.findByFilter(actionType, username, dateFrom, dateTo, pageable);

        model.addAttribute("historyPage", historyPage);
        model.addAttribute("actionType", actionType);
        model.addAttribute("username", username);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);

        return "main/history/user-account";
    }
}
