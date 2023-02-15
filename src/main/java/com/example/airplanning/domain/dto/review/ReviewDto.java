package com.example.airplanning.domain.dto.review;

import com.example.airplanning.domain.entity.Review;
import com.example.airplanning.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ReviewDto {

    private Long id;
    private Integer star;       // 별점
    private String title;       // 리뷰 제목
    private String content;     // 리뷰 내용
    private LocalDateTime createdAt;
    private String nickName; //작성자 닉네임
    private String userName; //작성자 아이디
    private String plannerName;
    private String image;
    private String userImage;   // 작성자 프로필

    public static ReviewDto of(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .star(review.getStar())
                .title(review.getTitle())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .userName(review.getUser().getUserName())
                .nickName(review.getUser().getNickname())
                .plannerName(review.getPlanner().getUser().getNickname())
                .image(review.getImage())
                .userImage(review.getUser().getImage())
                .build();
    }
}
