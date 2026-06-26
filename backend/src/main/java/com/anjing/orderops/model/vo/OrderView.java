package com.anjing.orderops.model.vo;

import com.anjing.orderops.enums.OrderStatus;
import com.anjing.orderops.enums.PaymentStatus;
import com.anjing.orderops.enums.ShipmentStatus;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderView {

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

    private List<String> riskTags;

    private List<OrderTimelineItem> timeline;

    private boolean canIntercept;

    private boolean canChangeAddress;

    private boolean canRefund;

    private boolean canCompensate;
}
