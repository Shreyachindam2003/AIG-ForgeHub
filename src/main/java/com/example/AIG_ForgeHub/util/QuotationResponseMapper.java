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

    private static final String QUOTATION_JSON_PREFIX="FORGEHUB_QUOTATION_V1:";

    private final ObjectMapper objectMapper;

    private final ModelMapper modelMapper;

    public RFQItemResponseDto toRFQItemResponse(RFQItem item) {
        return item==null ? null : modelMapper.map(item,RFQItemResponseDto.class);
    }

    public VendorQuotationResponseDto toVendorQuotationResponse(RFQQuotation quotation) {

        if(quotation==null) {
            return null;
        }

        ParsedQuotation parsed=parseQuotation(quotation);

        List<VendorQuotationHistory> history=new ArrayList<>();

        history.add(new VendorQuotationHistory(
                "Quotation Submitted",
                "CURRENT",
                quotation.getQuotedAmount(),
                quotation.getSubmittedDate(),
                quotation.getStatus()
        ));

        return VendorQuotationResponseDto.builder()
                .quotationId(quotation.getQuotationId())
                .bidNo(quotation.getBidNo())
                .rfqId(quotation.getRfq()!=null?quotation.getRfq().getRfqId():null)
                .rfqNo(quotation.getRfq()!=null?quotation.getRfq().getRfqNo():null)
                .indentNo(quotation.getRfq()!=null?quotation.getRfq().getIndentNo():null)
                .vendorId(quotation.getVendor()!=null?quotation.getVendor().getUserId():null)
                .vendorName(quotation.getVendor()!=null?quotation.getVendor().getFullName():null)
                .vendorEmail(quotation.getVendor()!=null?quotation.getVendor().getEmail():null)
                .quotedAmount(quotation.getQuotedAmount())
                .subtotal(parsed.subtotal())
                .gstRate(parsed.gstRate())
                .gstAmount(parsed.gstAmount())
                .grandTotal(parsed.grandTotal())
                .deliveryDate(quotation.getDeliveryDate())
                .paymentTerms(quotation.getPaymentTerms())
                .status(quotation.getStatus())
                .submittedDate(quotation.getSubmittedDate())
                .items(parsed.items())
                .history(history)
                .build();
    }

    public FinalizedQuotationResponseDto toFinalizedQuotationResponse(FinalizedQuotation finalized) {

        if(finalized==null) {
            return null;
        }

        RFQ rfq=finalized.getRfq();

        RFQQuotation quotation=finalized.getQuotation();

        List<RFQItemResponseDto> items=new ArrayList<>();

        if(rfq!=null && rfq.getItems()!=null) {

            for(RFQItem item:rfq.getItems()) {
                items.add(toRFQItemResponse(item));
            }
        }

        return FinalizedQuotationResponseDto.builder()
                .finalId(finalized.getFinalId())
                .rfqNo(rfq!=null?rfq.getRfqNo():null)
                .indentNo(rfq!=null?rfq.getIndentNo():null)
                .items(items)
                .vendorName(quotation!=null && quotation.getVendor()!=null?quotation.getVendor().getFullName():null)
                .quotationId(quotation!=null?quotation.getQuotationId():null)
                .bidNo(quotation!=null?quotation.getBidNo():null)
                .quotedAmount(quotation!=null?quotation.getQuotedAmount():null)
                .finalizedDate(finalized.getFinalizedDate())
                .build();
    }

    private ParsedQuotation parseQuotation(RFQQuotation quotation) {

        BigDecimal grandTotal=quotation.getQuotedAmount()==null
                ?BigDecimal.ZERO
                :quotation.getQuotedAmount();

        BigDecimal subtotal=BigDecimal.ZERO;

        BigDecimal gstAmount=BigDecimal.ZERO;

        BigDecimal gstRate=new BigDecimal("10");

        String remarks=quotation.getRemarks();

        List<VendorQuotationItemResponseDto> items=new ArrayList<>();

        String stored=quotation.getRemarks();

        if(stored!=null && stored.startsWith(QUOTATION_JSON_PREFIX)) {

            try {

                JsonNode root=objectMapper.readTree(
                        stored.substring(QUOTATION_JSON_PREFIX.length())
                );

                if(root.has("subtotal")) {
                    subtotal=root.get("subtotal").decimalValue();
                }

                if(root.has("gstAmount")) {
                    gstAmount=root.get("gstAmount").decimalValue();
                }

                if(root.has("grandTotal")) {
                    grandTotal=root.get("grandTotal").decimalValue();
                }

                if(root.has("gstRate")) {
                    gstRate=root.get("gstRate").decimalValue();
                }

                if(root.has("remarks") && !root.get("remarks").isNull()) {
                    remarks=root.get("remarks").asText();
                }

                JsonNode itemNodes=root.get("items");

                if(itemNodes!=null && itemNodes.isArray()) {

                    for(JsonNode item:itemNodes) {

                        BigDecimal itemSubtotal=
                                item.has("itemSubtotal")
                                        ?item.get("itemSubtotal").decimalValue()
                                        :BigDecimal.ZERO;

                        BigDecimal lineSubtotal=
                                item.has("subtotal")
                                        ?item.get("subtotal").decimalValue()
                                        :itemSubtotal;

                        BigDecimal unitPrice=
                                item.has("unitPrice")
                                        ?item.get("unitPrice").decimalValue()
                                        :BigDecimal.ZERO;

                        items.add(
                                VendorQuotationItemResponseDto.builder()
                                        .itemId(item.path("itemId").asLong())
                                        .itemName(item.path("itemName").asText("-"))
                                        .requiredQty(item.path("requiredQty").asInt())
                                        .availableQty(item.path("availableQty").asInt())
                                        .uom(item.path("uom").asText("-"))
                                        .unitPrice(unitPrice)
                                        .itemSubtotal(itemSubtotal)
                                        .subtotal(lineSubtotal)
                                        .build()
                        );
                    }
                }

            } catch(Exception ex) {

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