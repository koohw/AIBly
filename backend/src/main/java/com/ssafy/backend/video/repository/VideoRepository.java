package com.ssafy.backend.video.repository;

import com.ssafy.backend.domain.user.User;
import com.ssafy.backend.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> user(User user);
}
