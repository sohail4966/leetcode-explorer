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

    @Query(value = """
            SELECT DISTINCT p.* FROM problems p
            JOIN company_problems cp ON cp.problem_id = p.id
            JOIN companies c ON c.id = cp.company_id
            LEFT JOIN problem_topics pt ON pt.problem_id = p.id
            LEFT JOIN topics t ON t.id = pt.topic_id
            WHERE (:#{#companies == null} = true OR c.name IN :companies)
            AND (:#{#difficulties == null} = true OR p.difficulty IN :difficulties)
            AND (:#{#topics == null} = true OR t.name IN :topics)
            AND (:recency IS NULL OR cp.recency = :recency)
            AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (:minFrequency IS NULL OR cp.frequency >= :minFrequency)
            AND (:maxFrequency IS NULL OR cp.frequency <= :maxFrequency)
            ORDER BY (SELECT MAX(cp2.frequency) FROM company_problems cp2 WHERE cp2.problem_id = p.id) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM problems p
            JOIN company_problems cp ON cp.problem_id = p.id
            JOIN companies c ON c.id = cp.company_id
            LEFT JOIN problem_topics pt ON pt.problem_id = p.id
            LEFT JOIN topics t ON t.id = pt.topic_id
            WHERE (:#{#companies == null} = true OR c.name IN :companies)
            AND (:#{#difficulties == null} = true OR p.difficulty IN :difficulties)
            AND (:#{#topics == null} = true OR t.name IN :topics)
            AND (:recency IS NULL OR cp.recency = :recency)
            AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (:minFrequency IS NULL OR cp.frequency >= :minFrequency)
            AND (:maxFrequency IS NULL OR cp.frequency <= :maxFrequency)
            """,
            nativeQuery = true)
    Page<Problem> searchProblemsSortByFrequencyDesc(List<String> companies,
                                                     List<String> difficulties,
                                                     List<String> topics,
                                                     String recency,
                                                     String search,
                                                     Double minFrequency,
                                                     Double maxFrequency,
                                                     Pageable pageable);

    @Query(value = """
            SELECT DISTINCT p.* FROM problems p
            JOIN company_problems cp ON cp.problem_id = p.id
            JOIN companies c ON c.id = cp.company_id
            LEFT JOIN problem_topics pt ON pt.problem_id = p.id
            LEFT JOIN topics t ON t.id = pt.topic_id
            WHERE (:#{#companies == null} = true OR c.name IN :companies)
            AND (:#{#difficulties == null} = true OR p.difficulty IN :difficulties)
            AND (:#{#topics == null} = true OR t.name IN :topics)
            AND (:recency IS NULL OR cp.recency = :recency)
            AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (:minFrequency IS NULL OR cp.frequency >= :minFrequency)
            AND (:maxFrequency IS NULL OR cp.frequency <= :maxFrequency)
            ORDER BY (SELECT MAX(cp2.frequency) FROM company_problems cp2 WHERE cp2.problem_id = p.id) ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT p.id) FROM problems p
            JOIN company_problems cp ON cp.problem_id = p.id
            JOIN companies c ON c.id = cp.company_id
            LEFT JOIN problem_topics pt ON pt.problem_id = p.id
            LEFT JOIN topics t ON t.id = pt.topic_id
            WHERE (:#{#companies == null} = true OR c.name IN :companies)
            AND (:#{#difficulties == null} = true OR p.difficulty IN :difficulties)
            AND (:#{#topics == null} = true OR t.name IN :topics)
            AND (:recency IS NULL OR cp.recency = :recency)
            AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', CAST(:search AS text), '%')))
            AND (:minFrequency IS NULL OR cp.frequency >= :minFrequency)
            AND (:maxFrequency IS NULL OR cp.frequency <= :maxFrequency)
            """,
            nativeQuery = true)
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
