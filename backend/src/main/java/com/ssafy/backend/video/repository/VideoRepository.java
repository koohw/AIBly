package com.ssafy.backend.video.repository;

import com.ssafy.backend.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
