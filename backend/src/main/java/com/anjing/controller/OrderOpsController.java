package com.anjing.controller;

import com.anjing.model.constants.ApiConstants;
import com.anjing.model.response.APIResponse;
import com.anjing.orderops.enums.ApprovalStatus;
import com.anjing.orderops.enums.OrderStatus;
import com.anjing.orderops.model.dto.AgentExecuteRequest;
import com.anjing.orderops.model.dto.AgentPlanRequest;
import com.anjing.orderops.model.dto.ApprovalDecisionRequest;
import com.anjing.orderops.model.vo.AgentPlanView;
import com.anjing.orderops.model.vo.ApprovalTicketView;
import com.anjing.orderops.model.vo.AuditLogView;
import com.anjing.orderops.model.vo.CompensationTaskView;
import com.anjing.orderops.model.vo.ExecutionResultView;
import com.anjing.orderops.model.vo.OrderView;
import com.anjing.orderops.model.vo.ToolDefinitionView;
import com.anjing.orderops.service.OrderOpsAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Order Ops Agent", description = "订单运营执行 Agent 教学接口")
@RestController
@RequestMapping(ApiConstants.OrderOps.BASE)
@RequiredArgsConstructor
public class OrderOpsController {

    private final OrderOpsAgentService orderOpsAgentService;

    @Operation(summary = "查询订单列表", operationId = "listOrderOpsOrders")
    @GetMapping(ApiConstants.OrderOps.ORDERS)
    public APIResponse<List<OrderView>> listOrders(
            @Parameter(description = "订单号、客户名或手机号关键字")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "订单状态")
            @RequestParam(required = false) OrderStatus orderStatus) {
        return APIResponse.success(orderOpsAgentService.listOrders(keyword, orderStatus));
    }

    @Operation(summary = "查询订单详情", operationId = "getOrderOpsOrderDetail")
    @GetMapping(ApiConstants.OrderOps.ORDER_DETAIL)
    public APIResponse<OrderView> getOrderDetail(@PathVariable String orderNo) {
        return APIResponse.success(orderOpsAgentService.getOrder(orderNo));
    }

    @Operation(summary = "查询可用业务工具", operationId = "listOrderOpsTools")
    @GetMapping(ApiConstants.OrderOps.TOOLS)
    public APIResponse<List<ToolDefinitionView>> listTools() {
        return APIResponse.success(orderOpsAgentService.listTools());
    }

    @Operation(summary = "生成 Agent 执行计划", operationId = "planOrderOpsAgentAction")
    @PostMapping(ApiConstants.OrderOps.PLAN)
    public APIResponse<AgentPlanView> plan(@Valid @RequestBody AgentPlanRequest request) {
        return APIResponse.success(orderOpsAgentService.plan(request));
    }

    @Operation(summary = "执行 Agent 业务动作", operationId = "executeOrderOpsAgentAction")
    @PostMapping(ApiConstants.OrderOps.EXECUTE)
    public APIResponse<ExecutionResultView> execute(@Valid @RequestBody AgentExecuteRequest request) {
        return APIResponse.success(orderOpsAgentService.execute(request));
    }

    @Operation(summary = "查询审批单", operationId = "listOrderOpsApprovals")
    @GetMapping(ApiConstants.OrderOps.APPROVALS)
    public APIResponse<List<ApprovalTicketView>> listApprovals(
            @RequestParam(required = false) ApprovalStatus status) {
        return APIResponse.success(orderOpsAgentService.listApprovals(status));
    }

    @Operation(summary = "审批通过并继续执行", operationId = "confirmOrderOpsApproval")
    @PostMapping(ApiConstants.OrderOps.APPROVAL_CONFIRM)
    public APIResponse<ApprovalTicketView> confirmApproval(@PathVariable String approvalId,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        return APIResponse.success(orderOpsAgentService.confirmApproval(approvalId, request));
    }

    @Operation(summary = "审批拒绝", operationId = "rejectOrderOpsApproval")
    @PostMapping(ApiConstants.OrderOps.APPROVAL_REJECT)
    public APIResponse<ApprovalTicketView> rejectApproval(@PathVariable String approvalId,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        return APIResponse.success(orderOpsAgentService.rejectApproval(approvalId, request));
    }

    @Operation(summary = "查询执行审计日志", operationId = "listOrderOpsAuditLogs")
    @GetMapping(ApiConstants.OrderOps.AUDIT_LOGS)
    public APIResponse<List<AuditLogView>> listAuditLogs(
            @RequestParam(required = false) String orderNo,
            @RequestParam(defaultValue = "80") int limit) {
        return APIResponse.success(orderOpsAgentService.listAuditLogs(orderNo, limit));
    }

    @Operation(summary = "重试失败补偿", operationId = "retryOrderOpsCompensation")
    @PostMapping(ApiConstants.OrderOps.COMPENSATION_RETRY)
    public APIResponse<CompensationTaskView> retryCompensation(@PathVariable String compensationId,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        return APIResponse.success(orderOpsAgentService.retryCompensation(compensationId, request));
    }
}
