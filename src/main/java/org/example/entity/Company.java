package org.example.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @OneToMany(mappedBy = "company")
    private Set<CompanyProblem> companyProblems = new HashSet<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<CompanyProblem> getCompanyProblems() { return companyProblems; }
    public void setCompanyProblems(Set<CompanyProblem> companyProblems) { this.companyProblems = companyProblems; }
}
