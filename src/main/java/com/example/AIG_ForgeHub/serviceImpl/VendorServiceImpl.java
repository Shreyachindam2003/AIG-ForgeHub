package com.example.AIG_ForgeHub.serviceImpl;

import com.example.AIG_ForgeHub.dto.dashboardDto.*;
import com.example.AIG_ForgeHub.entity.*;
import com.example.AIG_ForgeHub.exception.BusinessException;
import com.example.AIG_ForgeHub.exception.ResourceNotFoundException;
import com.example.AIG_ForgeHub.repository.FinalizedQuotationRepository;
import com.example.AIG_ForgeHub.repository.RFQQuotationRepository;
import com.example.AIG_ForgeHub.repository.RFQVendorRepository;
import com.example.AIG_ForgeHub.repository.UserRepository;
import com.example.AIG_ForgeHub.service.VendorService;
import com.example.AIG_ForgeHub.util.QuotationResponseMapper;
import com.example.AIG_ForgeHub.enums.RFQStatus;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private static final BigDecimal GST_RATE=new BigDecimal("0.10");
    private static final String QUOTATION_JSON_PREFIX="FORGEHUB_QUOTATION_V1:";

    private final UserRepository userRepository;
    private final RFQVendorRepository rfqVendorRepository;
    private final RFQQuotationRepository rfqQuotationRepository;
    private final FinalizedQuotationRepository finalizedQuotationRepository;
    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;
    private final QuotationResponseMapper quotationResponseMapper;

    @Override
    @Transactional
    public UserResponseDto getVendor(Long vendorId) {
        User vendor=getVendorEntity(vendorId);
        return modelMapper.map(vendor,UserResponseDto.class);
    }

    private User getVendorEntity(Long vendorId) {
        User vendor=userRepository.findById(vendorId)
                .orElseThrow(()->new ResourceNotFoundException("Vendor was not found. Please select a valid vendor."));

        if(!"VENDOR".equals(vendor.getRole())) {
            throw new BusinessException("Selected user is not a vendor: "+vendor.getFullName());
        }

        return vendor;
    }

    @Override
    @Transactional
    public List<RFQResponseDto> getOpenRfqs(Long vendorId) {
        getVendorEntity(vendorId);

        List<RFQ> rfqs=rfqVendorRepository.findActiveAssignedRfqs(vendorId);
        List<RFQResponseDto> responseList=new ArrayList<>();

        LocalDate today=LocalDate.now();

        for(RFQ rfq:rfqs) {
            if(rfq.getExpiryDateOfBid()!=null&&today.isAfter(rfq.getExpiryDateOfBid().toLocalDate())) {
                continue;
            }

            if(rfq.getItems()!=null) {
                rfq.getItems().size();
            }

            responseList.add(modelMapper.map(rfq,RFQResponseDto.class));
        }

        return responseList;
    }

    @Override
    @Transactional
    public RFQResponseDto getAssignedRfq(Long rfqId, Long vendorId) {
        RFQ rfq=getAssignedRfqEntity(rfqId,vendorId);
        return modelMapper.map(rfq,RFQResponseDto.class);
    }

    private RFQ getAssignedRfqEntity(Long rfqId,Long vendorId) {
        getVendorEntity(vendorId);

        boolean assigned=rfqVendorRepository.existsByRfq_RfqIdAndVendor_UserId(rfqId,vendorId);

        if(!assigned) {
            throw new BusinessException("This RFQ is not assigned to the selected vendor");
        }

        RFQVendor rfqVendor=rfqVendorRepository.findByRfq_RfqIdAndVendor_UserId(rfqId,vendorId)
                .orElseThrow(()->new ResourceNotFoundException("RFQ assignment was not found for this vendor."));

        RFQ rfq=rfqVendor.getRfq();

        if(Boolean.TRUE.equals(rfq.getIsDeleted())) {
            throw new BusinessException("This RFQ is inactive");
        }

        if(rfq.getItems()!=null) {
            rfq.getItems().size();
        }

        return rfq;
    }

    @Override
    @Transactional
    public List<VendorQuotationResponseDto> getMySubmissions(Long vendorId) {
        getVendorEntity(vendorId);

        List<VendorQuotationResponseDto> responseList=new ArrayList<>();

        List<RFQQuotation> quotations=rfqQuotationRepository
                .findByVendor_UserIdOrderBySubmittedDateDesc(vendorId);

        for(RFQQuotation quotation:quotations) {
            if(quotation.getRfq()!=null&&quotation.getRfq().getItems()!=null) {
                quotation.getRfq().getItems().size();
            }

            responseList.add(quotationResponseMapper.toVendorQuotationResponse(quotation));
        }

        return responseList;
    }

    @Override
    @Transactional
    public void submitQuotation(Long rfqId, Long vendorId, VendorQuotationRequest request) {
        User vendor=getVendorEntity(vendorId);
        RFQ rfq=getAssignedRfqEntity(rfqId,vendorId);

        if(rfq.getStatus()!=RFQStatus.OPEN&&rfq.getStatus()!=RFQStatus.REOPENED) {
            throw new BusinessException("Quotation can only be submitted for an OPEN or REOPENED RFQ");
        }

        if(rfq.getExpiryDateOfBid()!=null&&LocalDate.now().isAfter(rfq.getExpiryDateOfBid().toLocalDate())) {
            throw new BusinessException("Bid submission date has expired");
        }

        if(request.getItems()==null||request.getItems().isEmpty()) {
            throw new BusinessException("Quotation must contain at least one item");
        }

        Map<Long,RFQItem> rfqItems=new LinkedHashMap<>();

        for(RFQItem item:rfq.getItems()) {
            rfqItems.put(item.getItemId(),item);
        }

        List<Map<String,Object>> quotationItems=new ArrayList<>();
        BigDecimal subtotal=BigDecimal.ZERO;

        for(VendorQuotationItemRequest itemRequest:request.getItems()) {
            RFQItem item=rfqItems.get(itemRequest.getItemId());

            if(item==null) {
                throw new BusinessException("Invalid RFQ item selected: "+itemRequest.getItemId());
            }

            int availableQty=0;

            if(itemRequest.getAvailableQty()!=null) {
                availableQty=itemRequest.getAvailableQty();
            }

            if(availableQty<0||(item.getReqQty()!=null&&availableQty>item.getReqQty())) {
                throw new BusinessException("Available quantity is invalid for item: "+item.getItemName());
            }

            BigDecimal unitPrice=itemRequest.getUnitPrice();

            if(unitPrice==null||unitPrice.compareTo(BigDecimal.ZERO)<0) {
                throw new BusinessException("Unit price is invalid for item: "+item.getItemName());
            }

            BigDecimal itemSubtotal=unitPrice.multiply(BigDecimal.valueOf(availableQty))
                    .setScale(4,RoundingMode.HALF_UP);

            subtotal=subtotal.add(itemSubtotal);

            Map<String,Object> line=new LinkedHashMap<>();
            line.put("itemId",item.getItemId());
            line.put("itemName",item.getItemName());
            line.put("requiredQty",item.getReqQty());
            line.put("availableQty",availableQty);
            line.put("uom",item.getUom());
            line.put("unitPrice",unitPrice);
            line.put("itemSubtotal",itemSubtotal);
            line.put("subtotal",itemSubtotal);

            quotationItems.add(line);
        }

        BigDecimal gst=subtotal.multiply(GST_RATE).setScale(4,RoundingMode.HALF_UP);
        BigDecimal grandTotal=subtotal.add(gst).setScale(4,RoundingMode.HALF_UP);

        Map<String,Object> quotationDetails=new LinkedHashMap<>();
        quotationDetails.put("gstRate",10);
        quotationDetails.put("subtotal",subtotal);
        quotationDetails.put("gstAmount",gst);
        quotationDetails.put("grandTotal",grandTotal);
        quotationDetails.put("items",quotationItems);

        String detailsJson;

        try {
            detailsJson=QUOTATION_JSON_PREFIX+objectMapper.writeValueAsString(quotationDetails);
        } catch(Exception e) {
            throw new BusinessException("Unable to prepare quotation details",e);
        }

        RFQQuotation quotation=rfqQuotationRepository
                .findByRfq_RfqIdAndVendor_UserId(rfqId,vendorId)
                .orElseGet(RFQQuotation::new);

        if(quotation.getBidNo()==null) {
            quotation.setBidNo("BID-"+rfq.getRfqNo()+"-V"+vendorId);
        }

        quotation.setQuotedAmount(grandTotal);
        quotation.setDeliveryDate(request.getDeliveryDate());
        quotation.setPaymentTerms(request.getPaymentTerms());
        quotation.setRemarks(detailsJson);
        quotation.setStatus("SUBMITTED");
        quotation.setSubmittedDate(LocalDateTime.now());
        quotation.setRfq(rfq);
        quotation.setVendor(vendor);

        rfqQuotationRepository.save(quotation);
    }



    @Override
    @Transactional
    public VendorQuotationResponseDto getMySubmission(Long quotationId, Long vendorId) {
        getVendorEntity(vendorId);

        RFQQuotation quotation=rfqQuotationRepository
                .findByQuotationIdAndVendor_UserId(quotationId,vendorId)
                .orElseThrow(()->new ResourceNotFoundException("Quotation was not found."));

        if(quotation.getRfq()!=null&&quotation.getRfq().getItems()!=null) {
            quotation.getRfq().getItems().size();
        }

        return quotationResponseMapper.toVendorQuotationResponse(quotation);
    }

    @Override
    @Transactional
    public List<FinalizedQuotationResponseDto> getFinalizedQuotations(Long vendorId) {
        getVendorEntity(vendorId);

        List<FinalizedQuotationResponseDto> responseList=new ArrayList<>();

        List<FinalizedQuotation> quotations=finalizedQuotationRepository
                .findByQuotation_Vendor_UserIdOrderByFinalizedDateDesc(vendorId);

        for(FinalizedQuotation quotation:quotations) {
            if(quotation.getRfq()!=null&&quotation.getRfq().getItems()!=null) {
                quotation.getRfq().getItems().size();
            }

            responseList.add(quotationResponseMapper.toFinalizedQuotationResponse(quotation));
        }

        return responseList;
    }

    @Override
    @Transactional
    public List<VendorQuotationResponseDto> getAllVendorQuotations() {
        List<VendorQuotationResponseDto> responseList=new ArrayList<>();

        List<RFQQuotation> quotations=rfqQuotationRepository.findAllByOrderBySubmittedDateDesc();

        for(RFQQuotation quotation:quotations) {
            if(quotation.getRfq()!=null&&quotation.getRfq().getItems()!=null) {
                quotation.getRfq().getItems().size();
            }

            responseList.add(quotationResponseMapper.toVendorQuotationResponse(quotation));
        }

        return responseList;
    }

    @Override
    @Transactional
    public VendorQuotationResponseDto getQuotationForAdmin(Long quotationId) {
        RFQQuotation quotation=getQuotationEntityForAdmin(quotationId);
        return quotationResponseMapper.toVendorQuotationResponse(quotation);
    }

    private RFQQuotation getQuotationEntityForAdmin(Long quotationId) {
        RFQQuotation quotation=rfqQuotationRepository.findById(quotationId)
                .orElseThrow(()->new ResourceNotFoundException("Vendor quotation was not found."));

        if(quotation.getRfq()!=null&&quotation.getRfq().getItems()!=null) {
            quotation.getRfq().getItems().size();
        }

        return quotation;
    }

    @Override
    @Transactional
    public List<FinalizedQuotationResponseDto> getAllFinalizedQuotations() {
        List<FinalizedQuotationResponseDto> responseList=new ArrayList<>();

        List<FinalizedQuotation> quotations=finalizedQuotationRepository.findAllByOrderByFinalizedDateDesc();

        for(FinalizedQuotation quotation:quotations) {
            if(quotation.getRfq()!=null&&quotation.getRfq().getItems()!=null) {
                quotation.getRfq().getItems().size();
            }

            responseList.add(quotationResponseMapper.toFinalizedQuotationResponse(quotation));
        }

        return responseList;
    }

    @Override
    @Transactional
    public void finalizeQuotation(Long quotationId) {
        RFQQuotation selectedQuotation=getQuotationEntityForAdmin(quotationId);

        if(finalizedQuotationRepository.findByQuotation_QuotationId(quotationId).isPresent()) {
            throw new BusinessException("This quotation is already finalized.");
        }

        RFQ rfq=selectedQuotation.getRfq();

        if(rfq==null) {
            throw new BusinessException("Quotation is not linked to an RFQ.");
        }

        if(Boolean.TRUE.equals(rfq.getIsDeleted())) {
            throw new BusinessException("Inactive RFQ cannot be finalized.");
        }

        if(rfq.getStatus()==RFQStatus.FINALIZED) {
            throw new BusinessException("This RFQ has already been finalized.");
        }

        List<RFQQuotation> quotations=rfqQuotationRepository.findByRfq_RfqId(rfq.getRfqId());

        for(RFQQuotation quotation:quotations) {
            if(quotation.getQuotationId().equals(quotationId)) {
                quotation.setStatus("FINALIZED");
            } else {
                quotation.setStatus("REJECTED");
            }

            rfqQuotationRepository.save(quotation);
        }

        rfq.setStatus(RFQStatus.FINALIZED);
        rfqQuotationRepository.save(selectedQuotation);

        FinalizedQuotation finalized=new FinalizedQuotation();
        finalized.setFinalizedDate(LocalDateTime.now());
        finalized.setRfq(rfq);
        finalized.setQuotation(selectedQuotation);

        finalizedQuotationRepository.save(finalized);
    }


}