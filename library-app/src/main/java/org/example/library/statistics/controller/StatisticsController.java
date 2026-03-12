package org.example.library.statistics.controller;

import lombok.RequiredArgsConstructor;
import org.example.library.security.UserDetailsImpl;
import org.example.library.security.UserPrincipal;
import org.example.library.statistics.dto.DashboardStatsDto;
import org.example.library.statistics.service.StatisticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService service;


    @GetMapping("/dashboard")
    public DashboardStatsDto getDashboardStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Integer year
    ) {
        return service.getDashboardStats(userPrincipal.getId(), year);
    }

}
