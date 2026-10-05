package com.example.AIG_ForgeHub.repository;

import com.example.AIG_ForgeHub.entity.RFQ;
import com.example.AIG_ForgeHub.entity.RFQVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RFQVendorRepository extends JpaRepository<RFQVendor,Long> {
    @Modifying
    @Query("DELETE FROM RFQVendor rv WHERE rv.rfq=:rfq")
    void deleteByRfq(@Param("rfq") RFQ rfq);

    @Query("""
            SELECT rv.rfq
            FROM RFQVendor rv
            WHERE rv.vendor.userId=:vendorId
              AND rv.rfq.isDeleted=false
              AND rv.rfq.status IN (com.example.AIG_ForgeHub.enums.RFQStatus.OPEN,
                                    com.example.AIG_ForgeHub.enums.RFQStatus.REOPENED)
            ORDER BY rv.rfq.rfqId DESC
            """)
    List<RFQ> findActiveAssignedRfqs(@Param("vendorId") Long vendorId);

    boolean existsByRfq_RfqIdAndVendor_UserId(Long rfqId,Long vendorId);

    Optional<RFQVendor> findByRfq_RfqIdAndVendor_UserId(Long rfqId,Long vendorId);
}