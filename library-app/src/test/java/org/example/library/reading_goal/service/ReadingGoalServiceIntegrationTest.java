package org.example.library.reading_goal.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.library.config.BaseIntegrationTest;
import org.example.library.exception.NotFoundException;
import org.example.library.reading_goal.domain.ReadingGoal;
import org.example.library.reading_goal.dto.ReadingGoalDto;
import org.example.library.reading_goal.repository.ReadingGoalRepository;
import org.example.library.user.domain.Role;
import org.example.library.user.domain.User;
import org.example.library.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class ReadingGoalServiceIntegrationTest extends BaseIntegrationTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ReadingGoalRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReadingGoalService service;


    @Test
    void shouldGetGoal() {
        var user = saveUser();
        var goal = ReadingGoal.builder()
                .user(user)
                .year(2024)
                .targetBooks(10)
                .targetPages(2000)
                .build();
        repository.save(goal);
        em.flush();
        em.clear();

        var result = service.getGoal(user.getId(), 2024);

        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getTargetBooks()).isEqualTo(10);
        assertThat(result.getTargetPages()).isEqualTo(2000);
    }

    @Test
    void shouldThrowNotFoundWhenGoalDoesNotExist() {
        var user = saveUser();

        assertThatThrownBy(() -> service.getGoal(user.getId(), 2024))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("error.reading_goal.not_found");
    }

    @Test
    void shouldCreateNewGoal() {
        var user = saveUser();
        var dto = ReadingGoalDto.builder()
                .year(2024)
                .targetBooks(15)
                .targetPages(3000)
                .build();

        var result = service.createOrUpdate(dto, user.getId());

        assertThat(result.getId()).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getTargetBooks()).isEqualTo(15);
        var savedGoal = repository.findByUserIdAndYear(user.getId(), 2024)
                .orElseThrow(() -> new AssertionError("Reading goal not found after creation"));
        assertThat(savedGoal.getTargetBooks()).isEqualTo(15);
    }

    @Test
    void shouldUpdateExistingGoal() {
        var user = saveUser();
        var goal = ReadingGoal.builder()
                .user(user)
                .year(2024)
                .targetBooks(10)
                .targetPages(2000)
                .build();
        repository.save(goal);
        em.flush();
        em.clear();
        var dto = ReadingGoalDto.builder()
                .year(2024)
                .targetBooks(20)
                .targetPages(4000)
                .build();

        var result = service.createOrUpdate(dto, user.getId());

        assertThat(result.getId()).isEqualTo(goal.getId());
        assertThat(result.getTargetBooks()).isEqualTo(20);
        var updatedGoal = repository.findById(goal.getId())
                .orElseThrow(() -> new AssertionError("Reading goal not found after update"));
        assertThat(updatedGoal.getTargetBooks()).isEqualTo(20);
    }

    @Test
    void shouldDeleteGoal() {
        var user = saveUser();
        var goal = ReadingGoal.builder()
                .user(user)
                .year(2024)
                .targetBooks(10)
                .build();
        repository.save(goal);
        em.flush();
        em.clear();

        service.delete(user.getId(), 2024);

        assertThat(repository.findByUserIdAndYear(user.getId(), 2024)).isEmpty();
    }


    private User saveUser() {
        var user = User.builder()
                .email("user@example.com")
                .fullName("User")
                .password("pass")
                .role(Role.USER)
                .build();
        return userRepository.save(user);
    }

}
