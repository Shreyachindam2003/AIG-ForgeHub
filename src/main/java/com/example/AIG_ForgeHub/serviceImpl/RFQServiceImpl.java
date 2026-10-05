package com.example.AIG_ForgeHub.serviceImpl;

import com.example.AIG_ForgeHub.dto.dashboardDto.*;
import com.example.AIG_ForgeHub.entity.RFQ;
import com.example.AIG_ForgeHub.entity.RFQItem;
import com.example.AIG_ForgeHub.entity.RFQVendor;
import com.example.AIG_ForgeHub.entity.User;
import com.example.AIG_ForgeHub.exception.BusinessException;
import com.example.AIG_ForgeHub.exception.ResourceNotFoundException;
import com.example.AIG_ForgeHub.repository.RFQItemRepository;
import com.example.AIG_ForgeHub.repository.RFQRepository;
import com.example.AIG_ForgeHub.repository.RFQVendorRepository;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.service.RFQService;
import com.example.AIG_ForgeHub.enums.RFQStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RFQServiceImpl implements RFQService {

    private final RFQRepository rfqRepository;
    private final RFQItemRepository rfqItemRepository;
    private final RFQVendorRepository rfqVendorRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public String generateRfqNo() {
        long nextNumber=rfqRepository.count()+1;
        return String.format("RFQ-%06d",nextNumber);
    }

    @Override
    public String generateIndentNo() {
        long nextNumber=rfqRepository.count()+1;
        return String.format("IND-%06d",nextNumber);
    }

    @Override
    @Transactional
    public void saveRfq(RFQCreateRequest request,Long adminUserId,boolean draft) {
        User admin=userRepository.findById(adminUserId)
                .orElseThrow(()->new ResourceNotFoundException("Admin user was not found. Please sign in again."));

        RFQ rfq=new RFQ();
        rfq.setRfqNo(generateRfqNo());
        rfq.setIndentNo(generateIndentNo());
        rfq.setContactPerson(request.getContactPerson());
        rfq.setMobile(request.getMobile());

        if(request.getBidDate()!=null) {
            rfq.setBidDate(request.getBidDate().atStartOfDay());
        }

        if(request.getExpiryDateOfBid()!=null) {
            rfq.setExpiryDateOfBid(request.getExpiryDateOfBid().atStartOfDay());
        }

        rfq.setStatus(draft?RFQStatus.DRAFT:RFQStatus.OPEN);
        rfq.setUser(admin);
        rfq.setIsDeleted(false);

        List<RFQItem> items=new ArrayList<>();

        if(request.getItems()!=null) {
            int lineNo=1;

            for(RFQItemRequest itemRequest:request.getItems()) {
                RFQItem item=modelMapper.map(itemRequest,RFQItem.class);
                item.setRfq(rfq);
                item.setRfqLineNo(lineNo);
                item.setItemNo(String.format("ITEM-%06d",lineNo));
                item.setFactoryCode(String.format("FAC-%06d",lineNo));
                items.add(item);
                lineNo++;
            }
        }

        rfq.setItems(items);

        RFQ savedRFQ=rfqRepository.save(rfq);

        if(request.getVendorIds()!=null) {
            for(Long vendorId:request.getVendorIds()) {
                User vendor=userRepository.findById(vendorId)
                        .orElseThrow(()->new ResourceNotFoundException("Vendor was not found: "+vendorId));

                if(!"VENDOR".equals(vendor.getRole())) {
                    throw new BusinessException("Selected user is not a vendor: "+vendor.getFullName());
                }

                RFQVendor rfqVendor=new RFQVendor();
                rfqVendor.setRfq(savedRFQ);
                rfqVendor.setVendor(vendor);
                rfqVendorRepository.save(rfqVendor);
            }
        }
    }

    @Override
    @Transactional
    public List<UserResponseDto> getAllVendors() {
        return userRepository.findByRole("VENDOR")
                .stream()
                .map(user->modelMapper.map(user, UserResponseDto.class))
                .toList();
    }

    @Override
    @Transactional
    public List<RFQResponseDto> getAllRFQs() {
        List<RFQ> rfqs=rfqRepository.findAllByOrderByRfqIdDesc();
        LocalDateTime now=LocalDateTime.now();

        for(RFQ rfq:rfqs) {
            if(rfq.getItems()!=null) {
                rfq.getItems().size();
            }

            if(rfq.getStatus()==RFQStatus.OPEN&&rfq.getExpiryDateOfBid()!=null&&now.isAfter(rfq.getExpiryDateOfBid())) {
                rfq.setStatus(RFQStatus.CLOSED);
                rfqRepository.save(rfq);
            }
        }

        return rfqs.stream()
                .map(rfq->modelMapper.map(rfq,RFQResponseDto.class))
                .toList();
    }

    @Override
    @Transactional
    public RFQResponseDto getRFQById(Long id) {
        RFQ rfq=rfqRepository.findByRfqIdAndIsDeletedFalse(id)
                .orElseThrow(()->new BusinessException("RFQ not found with ID: "+id));

        if(rfq.getItems()!=null) {
            rfq.getItems().size();
        }

        return modelMapper.map(rfq,RFQResponseDto.class);
    }

    @Override
    @Transactional
    public void softDeleteRFQ(Long id) {
        RFQ rfq=rfqRepository.findByRfqIdAndIsDeletedFalse(id)
                .orElseThrow(()->new BusinessException("RFQ not found with ID: "+id));

        rfq.setIsDeleted(true);
        rfqRepository.save(rfq);
    }

    @Override
    @Transactional
    public void updateRFQ(Long id, RFQCreateRequest request, boolean draft) {
        RFQ rfq=rfqRepository.findByRfqIdAndIsDeletedFalse(id)
                .orElseThrow(()->new BusinessException("RFQ not found with ID: "+id));

        rfq.setContactPerson(request.getContactPerson());
        rfq.setMobile(request.getMobile());

        if(request.getBidDate()!=null) {
            rfq.setBidDate(request.getBidDate().atStartOfDay());
        }

        if(request.getExpiryDateOfBid()!=null) {
            rfq.setExpiryDateOfBid(request.getExpiryDateOfBid().atStartOfDay());
        }

        rfqRepository.save(rfq);
    }

    @Override
    @Transactional
    public void updateRFQItem(RFQItemUpdateRequest request) {
        RFQItem item=rfqItemRepository.findById(request.getItemId())
                .orElseThrow(()->new BusinessException("RFQ Item not found with ID: "+request.getItemId()));

        item.setItemName(request.getItemName());
        item.setReqQty(request.getReqQty());
        item.setUom(request.getUom());
        item.setReqDeliveryDate(request.getReqDeliveryDate());
        item.setDeliveryLocation(request.getDeliveryLocation());
        item.setDescription(request.getDescription());

        rfqItemRepository.save(item);
    }

    @Override
    @Transactional
    public void openToRebid(Long id) {
        RFQ rfq=rfqRepository.findById(id)
                .orElseThrow(()->new BusinessException("RFQ not found with ID: "+id));

        if(Boolean.TRUE.equals(rfq.getIsDeleted())) {
            throw new BusinessException("Inactive RFQ cannot be opened for rebid.");
        }

        if(rfq.getStatus()!=RFQStatus.CLOSED) {
            throw new BusinessException("Only CLOSED RFQ can be opened for rebid.");
        }

        rfq.setStatus(RFQStatus.REOPENED);
        rfqRepository.save(rfq);
    }
}