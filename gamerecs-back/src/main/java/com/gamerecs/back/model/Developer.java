package com.gamerecs.back.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "developers")
@Data
@EqualsAndHashCode(exclude = "games")
@ToString(exclude = "games")
public class Developer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "developer_id")
    private Long developerId;

    @Column(name = "igdb_company_id", nullable = false, unique = true)
    private Long igdbCompanyId;

    @Column(nullable = false)
    private String name;

    @ManyToMany(mappedBy = "developers")
    private Set<Game> games = new HashSet<>();
} 
