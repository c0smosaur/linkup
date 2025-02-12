package com.core.linkup.club.clubnotice.response;

import com.core.linkup.club.clubnotice.entity.enums.NotificationType;
import com.core.linkup.common.entity.enums.OccupationType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ClubNoticeResponse(
        Long id,
        String title,
        String content,
        NotificationType type,
        Long writerId,
        String writerName,
        String writerUsername,
        String writerThumbnail,
        String writerOccupation,
        LocalDateTime date,
        List<ClubCommentResponse> comments

) {
}
