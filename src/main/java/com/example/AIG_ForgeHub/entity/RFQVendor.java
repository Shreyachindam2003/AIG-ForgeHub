package com.example.AIG_ForgeHub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="RFQVendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQVendor {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="Id")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="RFQId",nullable=false)
    private RFQ rfq;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="VendorId",nullable=false)
    private User vendor;
}