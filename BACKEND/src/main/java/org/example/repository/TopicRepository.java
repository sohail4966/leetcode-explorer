package org.example.repository;

import org.example.dto.TopicResponse;
import org.example.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findByName(String name);

    @Query("""
            SELECT new org.example.dto.TopicResponse(
                t.id, t.name, COUNT(p.id)
            )
            FROM Topic t
            LEFT JOIN t.problems p
            WHERE (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            GROUP BY t.id, t.name
            ORDER BY t.name
            """)
    List<TopicResponse> findAllWithProblemCount(String search);
}
