package org.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "problems")
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 500)
    private String title;

    @Column(nullable = false, length = 10)
    private String difficulty;

    @Column(name = "acceptance_rate")
    private Double acceptanceRate;

    @Column(nullable = false, unique = true, length = 500)
    private String link;

    @ManyToMany
    @JoinTable(
            name = "problem_topics",
            joinColumns = @JoinColumn(name = "problem_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    private Set<Topic> topics = new HashSet<>();

    @OneToMany(mappedBy = "problem")
    private Set<CompanyProblem> companyProblems = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Double getAcceptanceRate() { return acceptanceRate; }
    public void setAcceptanceRate(Double acceptanceRate) { this.acceptanceRate = acceptanceRate; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public Set<Topic> getTopics() { return topics; }
    public void setTopics(Set<Topic> topics) { this.topics = topics; }

    public Set<CompanyProblem> getCompanyProblems() { return companyProblems; }
    public void setCompanyProblems(Set<CompanyProblem> companyProblems) { this.companyProblems = companyProblems; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
