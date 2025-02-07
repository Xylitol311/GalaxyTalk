package com.example.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.function.Function;

@Getter
@AllArgsConstructor
public class CursorResponse<T> {
    private List<T> data;
    private String nextCursor;

    public static <T> CursorResponse<T> from(Slice<T> slice, Function<T, String> cursorExtractor) {
        List<T> content = slice.getContent();

        if (content.size() > slice.getPageable().getPageSize() - 1) {
            return new CursorResponse<>(content.subList(0, content.size() - 1), cursorExtractor.apply(content.get(content.size() - 1)));
        } else {
            return new CursorResponse<>(content, null);
        }
    }
}