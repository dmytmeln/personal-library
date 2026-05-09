package org.example.library.category.service;

import org.example.library.category.domain.Category;
import org.example.library.category.domain.CategoryTranslation;
import org.example.library.category.repository.CategoryRepository;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.exception.NotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CategoryServiceTest extends BaseIntegrationTest {

    @Autowired
    private CategoryRepository repository;

    @Autowired
    private CategoryService service;

    @BeforeAll
    static void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @Test
    void getById() {
        var categoryTranslation = CategoryTranslation.builder()
                .languageCode("en")
                .name("Category 1")
                .description("Description 1")
                .build();
        var category = Category.builder()
                .popularityCount(1)
                .translations(Map.of("en", categoryTranslation))
                .build();
        categoryTranslation.setCategory(category);
        var expected = repository.saveAndFlush(category);

        var existingCategory = service.getById(expected.getId());

        assertThat(existingCategory.name()).isEqualTo(expected.getTranslations().get("en").getName());
        assertThat(existingCategory.description()).isEqualTo(expected.getTranslations().get("en").getDescription());
    }

    @Test
    void getById_notFound() {
        assertThatThrownBy(() -> service.getById(-99999))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.category.not_found");
    }

}