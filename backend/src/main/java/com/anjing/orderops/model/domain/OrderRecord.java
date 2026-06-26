package com.anjing.orderops.model.domain;

import com.anjing.orderops.enums.OrderStatus;
import com.anjing.orderops.enums.PaymentStatus;
import com.anjing.orderops.enums.ShipmentStatus;
import com.anjing.orderops.model.vo.OrderTimelineItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRecord {

    private String orderNo;

    private String customerName;

    private String phoneMasked;

    private String address;

    private OrderStatus orderStatus;

    private PaymentStatus paymentStatus;

    private ShipmentStatus shipmentStatus;

    private BigDecimal paidAmount;

    private BigDecimal refundableAmount;

    private BigDecimal refundedAmount;

    private BigDecimal couponIssuedAmount;

    @Builder.Default
    private List<String> riskTags = new ArrayList<>();

    @Builder.Default
    private List<OrderTimelineItem> timeline = new ArrayList<>();
}
