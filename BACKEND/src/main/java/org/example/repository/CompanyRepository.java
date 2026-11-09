package org.example.repository;

import org.example.dto.CompanyResponse;
import org.example.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByName(String name);

    @Query("""
            SELECT new org.example.dto.CompanyResponse(
                c.id, c.name, COUNT(DISTINCT cp.problem.id)
            )
            FROM Company c
            LEFT JOIN c.companyProblems cp
            WHERE (:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            GROUP BY c.id, c.name
            ORDER BY c.name
            """)
    List<CompanyResponse> findAllWithProblemCount(String search);
}
