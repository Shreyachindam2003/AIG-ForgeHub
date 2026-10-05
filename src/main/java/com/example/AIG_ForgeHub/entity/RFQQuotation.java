package com.example.AIG_ForgeHub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="RFQQuotations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQQuotation {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="QuotationId")
    private Long quotationId;

    @Column(name="BidNo",columnDefinition="LONGTEXT")
    private String bidNo;

    @Column(name="QuotedAmount",precision=18,scale=4)
    private BigDecimal quotedAmount;

    @Column(name="DeliveryDate")
    private LocalDate deliveryDate;

    @Column(name="PaymentTerms",columnDefinition="LONGTEXT")
    private String paymentTerms;

    @Column(name="Remarks",columnDefinition="LONGTEXT")
    private String remarks;

    @Column(name="Status",columnDefinition="LONGTEXT")
    private String status;

    @Column(name="SubmittedDate")
    private LocalDateTime submittedDate;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="RFQId",nullable=false)
    private RFQ rfq;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="VendorId",nullable=false)
    private User vendor;
}