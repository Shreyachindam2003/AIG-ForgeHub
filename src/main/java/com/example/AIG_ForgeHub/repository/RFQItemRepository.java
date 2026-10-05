package com.example.AIG_ForgeHub.repository;

import com.example.AIG_ForgeHub.entity.RFQItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RFQItemRepository extends JpaRepository<RFQItem,Long> {
    List<RFQItem> findByRfq_RfqId(Long rfqId);
}