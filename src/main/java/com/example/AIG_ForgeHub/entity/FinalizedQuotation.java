package com.example.AIG_ForgeHub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name="FinalizedQuotations",uniqueConstraints={
        @UniqueConstraint(name="UK_Finalized_RFQ",columnNames="RFQId"),
        @UniqueConstraint(name="UK_Finalized_Quotation",columnNames="QuotationId")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinalizedQuotation {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="FinalId")
    private Long finalId;

    @Column(name="FinalizedDate")
    private LocalDateTime finalizedDate;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="RFQId",nullable=false)
    private RFQ rfq;

    @OneToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="QuotationId",nullable=false)
    private RFQQuotation quotation;
}