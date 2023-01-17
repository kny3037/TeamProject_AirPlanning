package com.example.airplanning.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Review extends BaseEntity{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer star;       // 별점
    private String title;       // 리뷰 제목
    private String content;     // 리뷰 내용
    private String image;       // 이미지 URL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;          // 리뷰를 작성한 유저

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planner_id")
    private Planner planner;    // 리뷰를 받은 플래너

    @OneToMany(mappedBy = "review")
    private List<Comment> comments;

    @OneToMany(mappedBy = "review")
    private List<Like> likes;
}