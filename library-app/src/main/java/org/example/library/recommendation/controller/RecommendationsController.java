package org.example.library.recommendation.controller;

import lombok.RequiredArgsConstructor;
import org.example.library.book.dto.BookDto;
import org.example.library.recommendation.service.RecommendationService;
import org.example.library.security.UserPrincipal;
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
    public List<BookDto> getRecommendations(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                            @RequestParam(required = false) Integer limit) {
        return recommendationService.getRecommendations(userPrincipal.getId(), limit);
    }

    @GetMapping("/popular")
    public List<BookDto> getPopularBooks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                         @RequestParam(required = false) Integer limit) {
        return recommendationService.getPopularBooks(userPrincipal.getId(), limit);
    }

    @GetMapping("/new")
    public List<BookDto> getNewArrivals(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                        @RequestParam(required = false) Integer limit) {
        return recommendationService.getNewArrivals(userPrincipal.getId(), limit);
    }

    @GetMapping("/trending-genres")
    public List<BookDto> getTrendingInFavoriteGenres(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                     @RequestParam(required = false) Integer limit) {
        return recommendationService.getTrendingInFavoriteGenres(userPrincipal.getId(), limit);
    }

    @GetMapping("/similar/{bookId}")
    public List<BookDto> getSimilarBooks(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                         @org.springframework.web.bind.annotation.PathVariable Integer bookId,
                                         @RequestParam(required = false) Integer limit) {
        return recommendationService.getSimilarBooks(bookId, userPrincipal.getId(), limit);
    }

    @GetMapping("/search-by-mood")
    public List<BookDto> searchByMood(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                      @RequestParam String query,
                                      @RequestParam(required = false) Integer limit) {
        return recommendationService.searchByMood(query, userPrincipal.getId(), limit);
    }

}
