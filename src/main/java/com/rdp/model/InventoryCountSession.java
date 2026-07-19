package com.rdp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rdp_inventory_count_sessions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"category_id", "version_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryCountSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(nullable = false)
    private Integer versionNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CountSessionStatus status = CountSessionStatus.DRAFT;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = true)
    private LocalDateTime submittedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", nullable = true)
    private User approvedBy;
    
    @Column(nullable = true)
    private LocalDateTime approvedAt;
    
    @Column(columnDefinition = "TEXT", nullable = true)
    private String rejectedReason;
    
    @Column(columnDefinition = "TEXT", nullable = true)
    private String overallComment;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InventoryCountLine> lines = new ArrayList<>();
}
