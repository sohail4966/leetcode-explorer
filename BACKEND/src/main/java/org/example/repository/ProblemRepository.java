package org.example.repository;

import org.example.entity.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Optional<Problem> findByTitle(String title);

    Optional<Problem> findByLink(String link);

    @Query("""
            SELECT DISTINCT p FROM Problem p
            JOIN p.companyProblems cp
            JOIN cp.company c
            LEFT JOIN p.topics t
            WHERE (:companies IS NULL OR c.name IN :companies)
            AND (:difficulties IS NULL OR p.difficulty IN :difficulties)
            AND (:topics IS NULL OR t.name IN :topics)
            AND (:recency IS NULL OR cp.recency = :recency)
            AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            AND (:minFrequency IS NULL OR cp.frequency >= :minFrequency)
            AND (:maxFrequency IS NULL OR cp.frequency <= :maxFrequency)
            """)
    Page<Problem> searchProblems(List<String> companies,
                                 List<String> difficulties,
                                 List<String> topics,
                                 String recency,
                                 String search,
                                 Double minFrequency,
                                 Double maxFrequency,
                                 Pageable pageable);

    @Query("""
            SELECT p FROM Problem p
            WHERE p.id IN (
                SELECT DISTINCT p2.id FROM Problem p2
                JOIN p2.companyProblems cp
                JOIN cp.company c
                LEFT JOIN p2.topics t
                WHERE (:companies IS NULL OR c.name IN :companies)
                AND (:difficulties IS NULL OR p2.difficulty IN :difficulties)
                AND (:topics IS NULL OR t.name IN :topics)
                AND (:recency IS NULL OR cp.recency = :recency)
                AND (:search IS NULL OR LOWER(p2.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                AND (:minFrequency IS NULL OR cp.frequency >= :minFrequency)
                AND (:maxFrequency IS NULL OR cp.frequency <= :maxFrequency)
            )
            ORDER BY (SELECT MAX(cp3.frequency) FROM CompanyProblem cp3 WHERE cp3.problem = p) DESC
            """)
    Page<Problem> searchProblemsSortByFrequencyDesc(List<String> companies,
                                                     List<String> difficulties,
                                                     List<String> topics,
                                                     String recency,
                                                     String search,
                                                     Double minFrequency,
                                                     Double maxFrequency,
                                                     Pageable pageable);

    @Query("""
            SELECT p FROM Problem p
            WHERE p.id IN (
                SELECT DISTINCT p2.id FROM Problem p2
                JOIN p2.companyProblems cp
                JOIN cp.company c
                LEFT JOIN p2.topics t
                WHERE (:companies IS NULL OR c.name IN :companies)
                AND (:difficulties IS NULL OR p2.difficulty IN :difficulties)
                AND (:topics IS NULL OR t.name IN :topics)
                AND (:recency IS NULL OR cp.recency = :recency)
                AND (:search IS NULL OR LOWER(p2.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                AND (:minFrequency IS NULL OR cp.frequency >= :minFrequency)
                AND (:maxFrequency IS NULL OR cp.frequency <= :maxFrequency)
            )
            ORDER BY (SELECT MAX(cp3.frequency) FROM CompanyProblem cp3 WHERE cp3.problem = p) ASC
            """)
    Page<Problem> searchProblemsSortByFrequencyAsc(List<String> companies,
                                                    List<String> difficulties,
                                                    List<String> topics,
                                                    String recency,
                                                    String search,
                                                    Double minFrequency,
                                                    Double maxFrequency,
                                                    Pageable pageable);

    @Query("""
            SELECT p FROM Problem p
            WHERE p.id IN (
                SELECT cp.problem.id FROM CompanyProblem cp
                WHERE cp.company.name IN :companies
                AND (:recency IS NULL OR cp.recency = :recency)
                GROUP BY cp.problem.id
                HAVING COUNT(DISTINCT cp.company.id) = :companyCount
            )
            AND (:difficulties IS NULL OR p.difficulty IN :difficulties)
            AND (:topics IS NULL OR EXISTS (
                SELECT 1 FROM p.topics t WHERE t.name IN :topics
            ))
            """)
    Page<Problem> findOverlap(List<String> companies,
                              long companyCount,
                              List<String> difficulties,
                              List<String> topics,
                              String recency,
                              Pageable pageable);
}
