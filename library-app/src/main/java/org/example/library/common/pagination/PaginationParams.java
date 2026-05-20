package org.example.library.common.pagination;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class PaginationParams {
    private Integer page;
    private Integer size;
    private List<String> sort;
}