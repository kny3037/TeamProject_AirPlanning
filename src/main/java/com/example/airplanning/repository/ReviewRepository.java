package com.example.airplanning.repository;

import com.example.airplanning.domain.entity.Review;
import com.example.airplanning.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findAllByUser(User user, Pageable pageable);

    Page<Review> findAllByTitleContains(String title, Pageable pageable);

    Page<Review> findAllByPlannerUserNicknameContains(String nickname, Pageable pageable);
}
