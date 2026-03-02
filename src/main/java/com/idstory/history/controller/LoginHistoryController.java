package com.idstory.history.controller;

import com.idstory.history.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 로그인 이력 컨트롤러
 *
 * <ul>
 *   <li>GET /history/login — 로그인 이력 목록 페이지</li>
 * </ul>
 */
@Controller
@RequestMapping("/history")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    private static final int PAGE_SIZE = 20;

    /** GET /history/login — 로그인 이력 목록 */
    @GetMapping("/login")
    public String list(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        var historyPage = loginHistoryService.findHistory(
                userId, actionType, dateFrom, dateTo,
                PageRequest.of(page, PAGE_SIZE));

        model.addAttribute("historyPage", historyPage);
        model.addAttribute("userId",      userId);
        model.addAttribute("actionType",  actionType);
        model.addAttribute("dateFrom",    dateFrom);
        model.addAttribute("dateTo",      dateTo);
        return "main/history/login";
    }
}
