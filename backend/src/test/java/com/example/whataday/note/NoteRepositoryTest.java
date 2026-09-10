package com.example.whataday.note;

import com.example.whataday.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoteRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private NoteRepository repository;

    @Test
    void tagsRoundTripThroughJsonColumn() {
        LocalDateTime noteTime = LocalDateTime.of(2026, 9, 10, 16, 30, 0);

        long id = repository.insert(new UserNote(null, noteTime,
                "和导师讨论了毕设方向", List.of("会议", "毕设"), null));

        UserNote saved = repository.findById(id).orElseThrow();
        assertThat(saved.content()).isEqualTo("和导师讨论了毕设方向");
        assertThat(saved.noteTime()).isEqualTo(noteTime);
        assertThat(saved.tags()).containsExactly("会议", "毕设");
    }

    @Test
    void findByDateOnlyReturnsThatDay() {
        repository.insert(new UserNote(null, LocalDateTime.of(2026, 9, 10, 9, 0), "早上记录", List.of(), null));
        repository.insert(new UserNote(null, LocalDateTime.of(2026, 9, 11, 9, 0), "次日记录", List.of(), null));

        assertThat(repository.findByDate(LocalDate.of(2026, 9, 10))).hasSize(1);
        assertThat(repository.findByDate(LocalDate.of(2026, 9, 10)).get(0).content()).isEqualTo("早上记录");
    }
}
