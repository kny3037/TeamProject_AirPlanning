package com.example.airplanning.domain.dto.board;

import com.example.airplanning.domain.entity.Board;
import com.example.airplanning.domain.entity.User;
import com.example.airplanning.domain.enum_class.Category;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@Builder
public class ReportCreateRequest {
    private String title;
    private String content;

    private Category category;

    public Board toEntity(User user, String image){
        return Board.builder()
                .user(user)
                .title(this.title)
                .content(this.content)
                .image(image)
                .category(Category.REPORT)
                .build();
    }
}


