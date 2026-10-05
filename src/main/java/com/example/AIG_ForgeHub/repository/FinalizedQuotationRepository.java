package com.example.AIG_ForgeHub.repository;

import com.example.AIG_ForgeHub.entity.FinalizedQuotation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FinalizedQuotationRepository extends JpaRepository<FinalizedQuotation,Long> {

    List<FinalizedQuotation> findByQuotation_Vendor_UserIdOrderByFinalizedDateDesc(Long vendorId);

    Optional<FinalizedQuotation> findByQuotation_QuotationIdAndQuotation_Vendor_UserId(Long quotationId,Long vendorId);

    Optional<FinalizedQuotation> findByRfq_RfqId(Long rfqId);

    Optional<FinalizedQuotation> findByQuotation_QuotationId(Long quotationId);

    List<FinalizedQuotation> findAllByOrderByFinalizedDateDesc();
}