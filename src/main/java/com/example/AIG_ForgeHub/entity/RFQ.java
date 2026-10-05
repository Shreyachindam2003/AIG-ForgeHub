package com.example.AIG_ForgeHub.entity;

import com.example.AIG_ForgeHub.enums.RFQStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="RFQs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQ {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="RFQId")
    private Long rfqId;

    @Column(name="RFQNo",nullable=false,columnDefinition="LONGTEXT")
    private String rfqNo;

    @Column(name="IndentNo",nullable=false,columnDefinition="LONGTEXT")
    private String indentNo;

    @Column(name="BidDate")
    private LocalDateTime bidDate;

    @Column(name="ExpiryDateofBid")
    private LocalDateTime expiryDateOfBid;

    @Column(name="ContactPerson",columnDefinition="LONGTEXT")
    private String contactPerson;

    @Column(name="Mobile",columnDefinition="LONGTEXT")
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(name="Status",nullable=false,columnDefinition="LONGTEXT")
    private RFQStatus status;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="UserId",nullable=false)
    private User user;

    @OneToMany(mappedBy="rfq",cascade=CascadeType.ALL,orphanRemoval=true)
    private List<RFQItem> items=new ArrayList<>();

    @Column(name="IsDeleted",nullable=false)
    private Boolean isDeleted=false;
}