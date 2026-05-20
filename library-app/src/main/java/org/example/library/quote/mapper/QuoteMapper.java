package org.example.library.quote.mapper;

import org.example.library.quote.domain.Quote;
import org.example.library.quote.dto.QuoteDto;
import org.example.library.quote.dto.QuoteRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface QuoteMapper {

    @Mapping(target = "libraryBookId", source = "libraryBook.id")
    QuoteDto toDto(Quote quote);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "libraryBook", ignore = true)
    Quote toEntity(QuoteRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "libraryBook", ignore = true)
    void update(@MappingTarget Quote quote, QuoteRequest request);

}
