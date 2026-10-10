
package com.example.AIG_ForgeHub.util;

import com.example.AIG_ForgeHub.dto.dashboardDto.*;
import com.example.AIG_ForgeHub.entity.FinalizedQuotation;
import com.example.AIG_ForgeHub.entity.RFQ;
import com.example.AIG_ForgeHub.entity.RFQItem;
import com.example.AIG_ForgeHub.entity.RFQQuotation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuotationResponseMapper {

    private static final String QUOTATION_JSON_PREFIX = "FORGEHUB_QUOTATION_V1:";

    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;

    public RFQItemResponseDto toRFQItemResponse(RFQItem item) {
        return item == null
                ? null
                : modelMapper.map(item, RFQItemResponseDto.class);
    }

    public VendorQuotationResponseDto toVendorQuotationResponse(
            RFQQuotation quotation) {

        if (quotation == null) {
            return null;
        }

        ParsedQuotation parsed = parseQuotation(quotation);

        VendorQuotationResponseDto dto =
                modelMapper.map(quotation, VendorQuotationResponseDto.class);

        RFQ rfq = quotation.getRfq();

        dto.setQuotationId(quotation.getQuotationId());
        dto.setBidNo(quotation.getBidNo());
        dto.setRfqId(rfq != null ? rfq.getRfqId() : null);
        dto.setRfqNo(rfq != null ? rfq.getRfqNo() : null);
        dto.setIndentNo(rfq != null ? rfq.getIndentNo() : null);

        dto.setVendorId(quotation.getVendor() != null
                ? quotation.getVendor().getUserId() : null);

        dto.setVendorName(quotation.getVendor() != null
                ? quotation.getVendor().getFullName() : null);

        dto.setVendorEmail(quotation.getVendor() != null
                ? quotation.getVendor().getEmail() : null);

        dto.setQuotedAmount(quotation.getQuotedAmount());
        dto.setSubtotal(parsed.subtotal());
        dto.setGstRate(parsed.gstRate());
        dto.setGstAmount(parsed.gstAmount());
        dto.setGrandTotal(parsed.grandTotal());
        dto.setDeliveryDate(quotation.getDeliveryDate());
        dto.setPaymentTerms(quotation.getPaymentTerms());
        dto.setStatus(quotation.getStatus());
        dto.setSubmittedDate(quotation.getSubmittedDate());
        dto.setItems(parsed.items());

        dto.setHistory(List.of(new VendorQuotationHistory(
                "Quotation Submitted",
                "CURRENT",
                quotation.getQuotedAmount(),
                quotation.getSubmittedDate(),
                quotation.getStatus()
        )));

        return dto;
    }

    public FinalizedQuotationResponseDto toFinalizedQuotationResponse(
            FinalizedQuotation finalized) {

        if (finalized == null) {
            return null;
        }

        FinalizedQuotationResponseDto dto =
                modelMapper.map(finalized, FinalizedQuotationResponseDto.class);

        RFQ rfq = finalized.getRfq();
        RFQQuotation quotation = finalized.getQuotation();

        dto.setFinalId(finalized.getFinalId());
        dto.setRfqNo(rfq != null ? rfq.getRfqNo() : null);
        dto.setIndentNo(rfq != null ? rfq.getIndentNo() : null);
        dto.setFinalizedDate(finalized.getFinalizedDate());

        List<RFQItemResponseDto> items = new ArrayList<>();

        if (rfq != null && rfq.getItems() != null) {
            for (RFQItem item : rfq.getItems()) {
                items.add(toRFQItemResponse(item));
            }
        }

        dto.setItems(items);

        dto.setVendorName(quotation != null && quotation.getVendor() != null
                ? quotation.getVendor().getFullName() : null);

        dto.setQuotationId(quotation != null
                ? quotation.getQuotationId() : null);

        dto.setBidNo(quotation != null
                ? quotation.getBidNo() : null);

        dto.setQuotedAmount(quotation != null
                ? quotation.getQuotedAmount() : null);

        return dto;
    }

    private ParsedQuotation parseQuotation(RFQQuotation quotation) {

        BigDecimal grandTotal = quotation.getQuotedAmount() != null
                ? quotation.getQuotedAmount()
                : BigDecimal.ZERO;

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal gstAmount = BigDecimal.ZERO;
        BigDecimal gstRate = new BigDecimal("10");

        String remarks = quotation.getRemarks();

        List<VendorQuotationItemResponseDto> items = new ArrayList<>();

        String stored = quotation.getRemarks();

        if (stored != null && stored.startsWith(QUOTATION_JSON_PREFIX)) {

            try {
                JsonNode root = objectMapper.readTree(
                        stored.substring(QUOTATION_JSON_PREFIX.length())
                );

                if (root.has("subtotal")) {
                    subtotal = root.get("subtotal").decimalValue();
                }

                if (root.has("gstAmount")) {
                    gstAmount = root.get("gstAmount").decimalValue();
                }

                if (root.has("grandTotal")) {
                    grandTotal = root.get("grandTotal").decimalValue();
                }

                if (root.has("gstRate")) {
                    gstRate = root.get("gstRate").decimalValue();
                }

                if (root.has("remarks") && !root.get("remarks").isNull()) {
                    remarks = root.get("remarks").asText();
                }

                JsonNode itemNodes = root.get("items");

                if (itemNodes != null && itemNodes.isArray()) {

                    for (JsonNode item : itemNodes) {

                        BigDecimal itemSubtotal = item.has("itemSubtotal")
                                ? item.get("itemSubtotal").decimalValue()
                                : BigDecimal.ZERO;

                        BigDecimal lineSubtotal = item.has("subtotal")
                                ? item.get("subtotal").decimalValue()
                                : itemSubtotal;

                        BigDecimal unitPrice = item.has("unitPrice")
                                ? item.get("unitPrice").decimalValue()
                                : BigDecimal.ZERO;

                        VendorQuotationItemResponseDto itemDto =
                                new VendorQuotationItemResponseDto();

                        itemDto.setItemId(item.path("itemId").asLong());
                        itemDto.setItemName(item.path("itemName").asText("-"));
                        itemDto.setRequiredQty(item.path("requiredQty").asInt());
                        itemDto.setAvailableQty(item.path("availableQty").asInt());
                        itemDto.setUom(item.path("uom").asText("-"));
                        itemDto.setUnitPrice(unitPrice);
                        itemDto.setItemSubtotal(itemSubtotal);
                        itemDto.setSubtotal(lineSubtotal);

                        items.add(itemDto);
                    }
                }

            } catch (Exception ex) {
                log.warn(
                        "Unable to parse stored quotation details for quotationId={}",
                        quotation.getQuotationId(),
                        ex
                );
            }
        }

        return new ParsedQuotation(
                subtotal,
                gstRate,
                gstAmount,
                grandTotal,
                remarks,
                items
        );
    }

    private record ParsedQuotation(
            BigDecimal subtotal,
            BigDecimal gstRate,
            BigDecimal gstAmount,
            BigDecimal grandTotal,
            String remarks,
            List<VendorQuotationItemResponseDto> items
    ) {
    }
}
