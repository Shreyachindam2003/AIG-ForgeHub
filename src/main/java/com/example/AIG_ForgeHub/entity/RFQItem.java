package com.example.AIG_ForgeHub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name="RFQItem")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQItem {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ItemId")
    private Long itemId;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="RFQId",nullable=false)
    private RFQ rfq;

    @Column(name="RFQLineNo")
    private Integer rfqLineNo;

    @Column(name="ItemNo",columnDefinition="LONGTEXT")
    private String itemNo;

    @Column(name="ItemName",columnDefinition="LONGTEXT")
    private String itemName;

    @Column(name="ReqQty")
    private Integer reqQty;

    @Column(name="UOM",columnDefinition="LONGTEXT")
    private String uom;

    @Column(name="ReqDeliveryDate")
    private LocalDate reqDeliveryDate;

    @Column(name="DeliveryLocation",columnDefinition="LONGTEXT")
    private String deliveryLocation;

    @Column(name="Description",columnDefinition="LONGTEXT")
    private String description;

    @Column(name="FactoryCode",columnDefinition="LONGTEXT")
    private String factoryCode;
}