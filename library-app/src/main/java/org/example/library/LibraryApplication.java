package org.example.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
@EnableScheduling
public class LibraryApplication {

    // TODO:
    //  додати всі книги автора до бібліотеки
    //  pin libraryBook/collection
    //  пошук колекцій
    //  category/author average rating
    //  review recommendations system
    //  optimize ui
    //  optimize server
    //  optimize db
    //  reduce duplication
    //  filter books by collections
    //  integrate docker to deployment process
    //  integration with external book APIs (Google Books, Open Library) for searching and adding books
    //  додати можливість додавати книги до бібліотеки за допомогою ISBN (запит до зовнішнього API для отримання даних про книгу)
    //  unit tests
    //  integration tests
    //  end-to-end tests

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }

}
