package org.example.library.reading_goal.controller;

import lombok.RequiredArgsConstructor;
import org.example.library.reading_goal.dto.ReadingGoalDto;
import org.example.library.reading_goal.service.ReadingGoalService;
import org.example.library.security.UserDetailsImpl;
import org.example.library.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reading-goals")
@RequiredArgsConstructor
public class ReadingGoalController {

    private final ReadingGoalService service;


    @GetMapping
    public ReadingGoalDto getGoal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Integer year
    ) {
        return service.getGoal(userPrincipal.getId(), year);
    }

    @PutMapping
    public ReadingGoalDto saveOrUpdate(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody ReadingGoalDto dto
    ) {
        return service.createOrUpdate(dto, userPrincipal.getId());
    }

    @DeleteMapping
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam Integer year
    ) {
        service.delete(userPrincipal.getId(), year);
    }

}
