package com.anjing.orderops.service;

import com.anjing.model.errorcode.OrderOpsErrorCode;
import com.anjing.model.exception.BizException;
import com.anjing.orderops.enums.OrderActionType;
import com.anjing.orderops.enums.OrderStatus;
import com.anjing.orderops.enums.PaymentStatus;
import com.anjing.orderops.enums.RiskLevel;
import com.anjing.orderops.enums.ShipmentStatus;
import com.anjing.orderops.enums.ToolCallStatus;
import com.anjing.orderops.model.domain.OrderRecord;
import com.anjing.orderops.model.domain.ToolStepDefinition;
import com.anjing.orderops.model.dto.AgentPlanRequest;
import com.anjing.orderops.model.vo.OrderTimelineItem;
import com.anjing.orderops.model.vo.ToolCallView;
import com.anjing.orderops.model.vo.ToolDefinitionView;
import com.anjing.util.DateUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderOpsToolRegistry {

    private static final String QUERY_ORDER_CONTEXT = "query_order_context";
    private static final String INTERCEPT_SHIPMENT = "intercept_shipment";
    private static final String CHANGE_ADDRESS = "change_address";
    private static final String APPLY_REFUND = "apply_refund";
    private static final String ISSUE_COUPON = "issue_coupon";
    private static final String WRITE_OPERATION_AUDIT = "write_operation_audit";

    private final OrderOpsStore store;

    public List<ToolDefinitionView> listTools() {
        return List.of(
                ToolDefinitionView.builder()
                        .toolName(QUERY_ORDER_CONTEXT)
                        .title("订单上下文查询")
                        .actionType(OrderActionType.QUERY_ORDER)
                        .riskLevel(RiskLevel.LOW)
                        .description("读取订单、支付、物流和售后上下文，不产生业务副作用。")
                        .requiresApprovalHint("只读查询不需要审批。")
                        .idempotencyHint("只读工具可以重复调用。")
                        .build(),
                ToolDefinitionView.builder()
                        .toolName(INTERCEPT_SHIPMENT)
                        .title("发货拦截")
                        .actionType(OrderActionType.INTERCEPT_SHIPMENT)
                        .riskLevel(RiskLevel.MEDIUM)
                        .description("在发货链路中尝试拦截包裹，成功后订单进入已拦截状态。")
                        .requiresApprovalHint("运输中包裹需要人工确认。")
                        .idempotencyHint("使用业务幂等键避免重复拦截。")
                        .build(),
                ToolDefinitionView.builder()
                        .toolName(CHANGE_ADDRESS)
                        .title("地址修改")
                        .actionType(OrderActionType.CHANGE_ADDRESS)
                        .riskLevel(RiskLevel.MEDIUM)
                        .description("将订单收货地址改为客户确认后的新地址。")
                        .requiresApprovalHint("仓内拣货后改地址需要人工确认。")
                        .idempotencyHint("同一幂等键只能绑定同一次地址变更。")
                        .build(),
                ToolDefinitionView.builder()
                        .toolName(APPLY_REFUND)
                        .title("退款申请")
                        .actionType(OrderActionType.APPLY_REFUND)
                        .riskLevel(RiskLevel.HIGH)
                        .description("发起售后退款并扣减订单可退金额。")
                        .requiresApprovalHint("超过 100 元或已发货订单需要人工确认。")
                        .idempotencyHint("退款金额和订单号共同参与幂等指纹。")
                        .build(),
                ToolDefinitionView.builder()
                        .toolName(ISSUE_COUPON)
                        .title("优惠补偿")
                        .actionType(OrderActionType.ISSUE_COUPON)
                        .riskLevel(RiskLevel.LOW)
                        .description("向客户发放优惠补偿券，用于低风险体验补偿。")
                        .requiresApprovalHint("超过 50 元的补偿券需要人工确认。")
                        .idempotencyHint("补偿金额和原因共同参与幂等指纹。")
                        .build(),
                ToolDefinitionView.builder()
                        .toolName(WRITE_OPERATION_AUDIT)
                        .title("执行审计写入")
                        .actionType(OrderActionType.QUERY_ORDER)
                        .riskLevel(RiskLevel.LOW)
                        .description("每次执行收口前写入统一审计轨迹。")
                        .requiresApprovalHint("审计写入不需要审批。")
                        .idempotencyHint("审计记录跟随执行结果，不单独复用幂等键。")
                        .build());
    }

    public List<ToolStepDefinition> planSteps(AgentPlanRequest request, OrderRecord order) {
        List<ToolStepDefinition> steps = new ArrayList<>();
        steps.add(step(QUERY_ORDER_CONTEXT, "读取订单上下文",
                "订单 " + order.getOrderNo() + " / " + order.getShipmentStatus()));
        switch (request.getActionType()) {
            case QUERY_ORDER -> {
                return steps;
            }
            case INTERCEPT_SHIPMENT -> steps.add(step(INTERCEPT_SHIPMENT, "调用发货拦截工具",
                    "物流状态 " + order.getShipmentStatus()));
            case CHANGE_ADDRESS -> steps.add(step(CHANGE_ADDRESS, "调用地址修改工具",
                    "新地址：" + request.getNewAddress()));
            case APPLY_REFUND -> steps.add(step(APPLY_REFUND, "调用退款申请工具",
                    "退款金额：" + request.getRefundAmount()));
            case ISSUE_COUPON -> steps.add(step(ISSUE_COUPON, "调用优惠补偿工具",
                    "补偿金额：" + request.getCouponAmount()));
            default -> throw new BizException(OrderOpsErrorCode.ACTION_NOT_ALLOWED);
        }
        steps.add(step(WRITE_OPERATION_AUDIT, "写入执行审计",
                "动作：" + request.getActionType()));
        return steps;
    }

    public List<ToolCallView> toPendingCalls(List<ToolStepDefinition> steps) {
        return steps.stream()
                .map(step -> ToolCallView.builder()
                        .toolName(step.getToolName())
                        .stepName(step.getStepName())
                        .status(ToolCallStatus.PENDING)
                        .inputSummary(step.getInputSummary())
                        .build())
                .toList();
    }

    public String executeStep(ToolStepDefinition step, AgentPlanRequest request, OrderRecord order) {
        return switch (step.getToolName()) {
            case QUERY_ORDER_CONTEXT -> "读取到订单状态 " + order.getOrderStatus()
                    + "，物流状态 " + order.getShipmentStatus();
            case INTERCEPT_SHIPMENT -> interceptShipment(order);
            case CHANGE_ADDRESS -> changeAddress(order, request.getNewAddress());
            case APPLY_REFUND -> applyRefund(order, request.getRefundAmount());
            case ISSUE_COUPON -> issueCoupon(order, request.getCouponAmount());
            case WRITE_OPERATION_AUDIT -> "执行链路已进入统一审计日志";
            default -> throw new BizException(OrderOpsErrorCode.TOOL_EXECUTION_FAILED);
        };
    }

    public String compensationAction(OrderActionType actionType) {
        return switch (actionType) {
            case QUERY_ORDER -> "只读查询无需补偿";
            case INTERCEPT_SHIPMENT -> "补偿动作：释放拦截标记并恢复发货队列";
            case CHANGE_ADDRESS -> "补偿动作：回滚至上一次确认地址";
            case APPLY_REFUND -> "补偿动作：撤销退款申请并恢复可退金额";
            case ISSUE_COUPON -> "补偿动作：作废未使用补偿券";
        };
    }

    public boolean isMutationTool(String toolName) {
        return INTERCEPT_SHIPMENT.equals(toolName)
                || CHANGE_ADDRESS.equals(toolName)
                || APPLY_REFUND.equals(toolName)
                || ISSUE_COUPON.equals(toolName);
    }

    private ToolStepDefinition step(String toolName, String stepName, String inputSummary) {
        return ToolStepDefinition.builder()
                .toolName(toolName)
                .stepName(stepName)
                .inputSummary(inputSummary)
                .build();
    }

    private String interceptShipment(OrderRecord order) {
        order.setShipmentStatus(ShipmentStatus.INTERCEPTED);
        order.setOrderStatus(OrderStatus.FULFILLING);
        appendTimeline(order, "发货拦截成功", "包裹已从履约链路中拦截");
        store.saveOrder(order);
        return "订单已拦截，等待运营确认后续处理";
    }

    private String changeAddress(OrderRecord order, String newAddress) {
        String oldAddress = order.getAddress();
        order.setAddress(newAddress);
        appendTimeline(order, "地址修改成功", "从 " + oldAddress + " 修改为 " + newAddress);
        store.saveOrder(order);
        return "收货地址已更新";
    }

    private String applyRefund(OrderRecord order, BigDecimal refundAmount) {
        BigDecimal currentRefunded = safeAmount(order.getRefundedAmount());
        BigDecimal currentRefundable = safeAmount(order.getRefundableAmount());
        order.setRefundedAmount(currentRefunded.add(refundAmount));
        order.setRefundableAmount(currentRefundable.subtract(refundAmount).max(BigDecimal.ZERO));
        order.setPaymentStatus(order.getRefundableAmount().compareTo(BigDecimal.ZERO) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        appendTimeline(order, "退款申请成功", "退款金额 " + refundAmount + " 元");
        store.saveOrder(order);
        return "退款申请已创建，可退金额剩余 " + order.getRefundableAmount() + " 元";
    }

    private String issueCoupon(OrderRecord order, BigDecimal couponAmount) {
        BigDecimal issued = safeAmount(order.getCouponIssuedAmount());
        order.setCouponIssuedAmount(issued.add(couponAmount));
        appendTimeline(order, "优惠补偿成功", "补偿券金额 " + couponAmount + " 元");
        store.saveOrder(order);
        return "补偿券已发放，累计补偿 " + order.getCouponIssuedAmount() + " 元";
    }

    private void appendTimeline(OrderRecord order, String title, String detail) {
        order.getTimeline().add(OrderTimelineItem.builder()
                .time(DateUtils.nowIso())
                .title(title)
                .detail(detail)
                .build());
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
