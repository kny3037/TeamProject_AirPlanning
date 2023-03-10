package com.example.airplanning.domain.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class CommentDeleteRequest {
    private Long targetCommentId;
    private Long postId;
}
