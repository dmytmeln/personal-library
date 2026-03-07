package org.example.library.recommendations.controller;

import lombok.RequiredArgsConstructor;
import org.example.library.book.dto.BookDto;
import org.example.library.recommendations.service.RecommendationService;
import org.example.library.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationsController {

    private final RecommendationService recommendationService;


    @GetMapping
    public List<BookDto> getRecommendations(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                            @RequestParam(required = false) Integer limit) {
        return recommendationService.getRecommendations(userDetails.getId(), limit);
    }

    @GetMapping("/popular")
    public List<BookDto> getPopularBooks(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                         @RequestParam(required = false) Integer limit) {
        return recommendationService.getPopularBooks(userDetails.getId(), limit);
    }

    @GetMapping("/new")
    public List<BookDto> getNewArrivals(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                         @RequestParam(required = false) Integer limit) {
        return recommendationService.getNewArrivals(userDetails.getId(), limit);
    }

    @GetMapping("/trending-genres")
    public List<BookDto> getTrendingInFavoriteGenres(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                      @RequestParam(required = false) Integer limit) {
        return recommendationService.getTrendingInFavoriteGenres(userDetails.getId(), limit);
    }
    @GetMapping("/similar/{bookId}")
    public List<BookDto> getSimilarBooks(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                         @org.springframework.web.bind.annotation.PathVariable Integer bookId,
                                         @RequestParam(required = false) Integer limit) {
        return recommendationService.getSimilarBooks(bookId, userDetails.getId(), limit);
    }

}
