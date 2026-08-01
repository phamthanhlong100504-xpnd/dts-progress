package com.dts.progress.mapper;

import com.dts.progress.dto.response.ChapterProgressResponse;
import com.dts.progress.dto.response.SessionHistoryResponse;
import com.dts.progress.entity.ChapterProgress;
import com.dts.progress.entity.StudySession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProgressMapper {

    @Mapping(target = "completionPercent", expression = "java(computeCompletionPercent(entity))")
    ChapterProgressResponse toChapterProgressResponse(ChapterProgress entity);

    SessionHistoryResponse toSessionHistoryResponse(StudySession entity);

    default Double computeCompletionPercent(ChapterProgress entity) {
        if (entity.getQuestionsTotal() == 0) return 0.0;
        return Math.round((double) entity.getQuestionsAnswered() / entity.getQuestionsTotal() * 10000.0) / 100.0;
    }
}
