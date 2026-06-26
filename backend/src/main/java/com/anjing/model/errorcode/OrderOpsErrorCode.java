package com.anjing.model.errorcode;

/**
 * 订单运营执行 Agent 错误码。
 */
public enum OrderOpsErrorCode implements ErrorCode {

    ORDER_NOT_FOUND("2400", "订单不存在"),
    ACTION_NOT_ALLOWED("2401", "当前订单状态不允许执行该动作"),
    APPROVAL_REQUIRED("2402", "该动作需要人工审批"),
    APPROVAL_NOT_FOUND("2403", "审批单不存在"),
    APPROVAL_ALREADY_HANDLED("2404", "审批单已处理"),
    IDEMPOTENCY_CONFLICT("2405", "幂等键已被其他请求占用"),
    COMPENSATION_NOT_FOUND("2406", "补偿任务不存在"),
    COMPENSATION_NOT_RETRYABLE("2407", "补偿任务不可重试"),
    TOOL_EXECUTION_FAILED("2408", "工具执行失败"),
    INVALID_ACTION_PAYLOAD("2409", "动作参数不完整");

    private final String code;
    private final String message;

    OrderOpsErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
