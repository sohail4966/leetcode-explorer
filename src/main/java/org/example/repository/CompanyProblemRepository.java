package org.example.repository;

import org.example.entity.CompanyProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CompanyProblemRepository extends JpaRepository<CompanyProblem, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM CompanyProblem cp WHERE cp.company.id = :companyId")
    void deleteAllByCompanyId(Long companyId);

    @Query("SELECT COUNT(DISTINCT cp.problem.id) FROM CompanyProblem cp WHERE cp.company.name = :companyName")
    long countDistinctProblemsByCompanyName(String companyName);

    @Query("""
            SELECT p.difficulty, COUNT(DISTINCT cp.problem.id)
            FROM CompanyProblem cp JOIN cp.problem p
            WHERE cp.company.name = :companyName
            GROUP BY p.difficulty
            """)
    List<Object[]> difficultyBreakdown(String companyName);

    @Query("""
            SELECT cp.recency, COUNT(DISTINCT cp.problem.id)
            FROM CompanyProblem cp
            WHERE cp.company.name = :companyName
            GROUP BY cp.recency
            """)
    List<Object[]> recencyBreakdown(String companyName);

    @Query("""
            SELECT t.name, COUNT(DISTINCT cp.problem.id)
            FROM CompanyProblem cp
            JOIN cp.problem p
            JOIN p.topics t
            WHERE cp.company.name = :companyName
            GROUP BY t.name
            ORDER BY COUNT(DISTINCT cp.problem.id) DESC
            LIMIT 10
            """)
    List<Object[]> topTopics(String companyName);

    List<CompanyProblem> findByProblemIdAndCompanyNameIn(Long problemId, List<String> companyNames);

    @Query("""
            SELECT cp FROM CompanyProblem cp
            JOIN FETCH cp.company
            WHERE cp.problem.id = :problemId
            ORDER BY cp.frequency DESC
            """)
    List<CompanyProblem> findByProblemIdOrderByFrequencyDesc(Long problemId);
}
