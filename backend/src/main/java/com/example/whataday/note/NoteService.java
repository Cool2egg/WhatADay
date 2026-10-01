package com.example.whataday.note;

import com.example.whataday.common.NotFoundException;
import com.example.whataday.memory.WorkMemoryIndexer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 用户手动记录。 */
@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final Optional<WorkMemoryIndexer> memoryIndexer;

    public NoteService(NoteRepository noteRepository) {
        this(noteRepository, Optional.empty());
    }

    @Autowired
    public NoteService(NoteRepository noteRepository, Optional<WorkMemoryIndexer> memoryIndexer) {
        this.noteRepository = noteRepository;
        this.memoryIndexer = memoryIndexer;
    }

    /** 新增一条手动记录并返回落库后的完整对象。 */
    public UserNote create(LocalDateTime noteTime, String content, List<String> tags) {
        LocalDateTime time = noteTime != null ? noteTime : LocalDateTime.now();
        long id = noteRepository.insert(new UserNote(null, time, content, tags, null));
        UserNote saved = noteRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("记录写入后未能读回，id=" + id));
        memoryIndexer.ifPresent(indexer -> indexer.indexNote(saved));
        return saved;
    }

    /** 某一天的手动记录。 */
    public List<UserNote> byDate(LocalDate date) {
        return noteRepository.findByDate(date != null ? date : LocalDate.now());
    }
}
