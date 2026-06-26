<template>
  <div class="order-ops-page">
    <section class="ops-header">
      <div>
        <p class="ops-eyebrow">Order Ops Agent</p>
        <h1>订单运营执行 Agent</h1>
        <div class="header-tags">
          <ElTag effect="dark" round>Tool Calling</ElTag>
          <ElTag type="success" round>审批边界</ElTag>
          <ElTag type="warning" round>幂等重放</ElTag>
          <ElTag type="info" round>补偿审计</ElTag>
        </div>
      </div>
      <ElButton :icon="Refresh" :loading="pageLoading" @click="loadAll">刷新</ElButton>
    </section>

    <section class="metrics-strip">
      <div v-for="metric in metrics" :key="metric.label" class="metric-tile">
        <ElIcon :class="['metric-icon', metric.tone]">
          <component :is="metric.icon" />
        </ElIcon>
        <div>
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </div>
    </section>

    <section class="ops-grid">
      <div class="panel order-panel">
        <div class="panel-heading">
          <div>
            <p>订单池</p>
            <strong>{{ selectedOrderNo || '未选择订单' }}</strong>
          </div>
          <ElTag
            v-if="selectedOrder?.orderStatus"
            :type="orderStatusTag(selectedOrder.orderStatus).type"
          >
            {{ orderStatusTag(selectedOrder.orderStatus).label }}
          </ElTag>
        </div>

        <ElForm class="search-form" :model="searchForm" inline>
          <ElFormItem>
            <ElInput
              v-model="searchForm.keyword"
              clearable
              placeholder="订单号 / 客户"
              :prefix-icon="Search"
              @keyup.enter="loadOrders"
            />
          </ElFormItem>
          <ElFormItem>
            <ElSelect
              v-model="searchForm.orderStatus"
              clearable
              placeholder="状态"
              class="status-filter"
            >
              <ElOption
                v-for="option in orderStatusOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem>
            <ElButton :icon="Search" @click="loadOrders">查询</ElButton>
          </ElFormItem>
        </ElForm>

        <ElTable
          v-loading="orderLoading"
          :data="orders"
          height="352"
          highlight-current-row
          :row-class-name="orderRowClassName"
          @row-click="handleOrderRowClick"
        >
          <ElTableColumn label="订单" min-width="188">
            <template #default="{ row }">
              <div class="order-main">
                <strong>{{ row.orderNo }}</strong>
                <span>{{ row.customerName }} · {{ row.phoneMasked }}</span>
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="金额" width="112">
            <template #default="{ row }">
              <span>{{ formatMoney(row.paidAmount) }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="物流" width="118">
            <template #default="{ row }">
              <ElTag size="small" :type="shipmentStatusTag(row.shipmentStatus).type">
                {{ shipmentStatusTag(row.shipmentStatus).label }}
              </ElTag>
            </template>
          </ElTableColumn>
        </ElTable>

        <div v-if="selectedOrder" class="order-detail">
          <div class="detail-line">
            <ElIcon><Location /></ElIcon>
            <span>{{ selectedOrder.address }}</span>
          </div>
          <div class="capability-row">
            <ElTag :type="selectedOrder.canIntercept ? 'success' : 'info'">发货拦截</ElTag>
            <ElTag :type="selectedOrder.canChangeAddress ? 'success' : 'info'">地址修改</ElTag>
            <ElTag :type="selectedOrder.canRefund ? 'success' : 'info'">退款申请</ElTag>
            <ElTag :type="selectedOrder.canCompensate ? 'success' : 'info'">优惠补偿</ElTag>
          </div>
          <ElTimeline class="order-timeline">
            <ElTimelineItem
              v-for="item in selectedOrder.timeline || []"
              :key="`${item.time}-${item.title}`"
              :timestamp="formatTime(item.time)"
              placement="top"
            >
              <strong>{{ item.title }}</strong>
              <span>{{ item.detail }}</span>
            </ElTimelineItem>
          </ElTimeline>
        </div>
      </div>

      <div class="panel execution-panel">
        <div class="panel-heading">
          <div>
            <p>动作编排</p>
            <strong>{{ actionLabel(actionForm.actionType) }}</strong>
          </div>
          <ElTag v-if="currentPlan?.riskLevel" :type="riskTag(currentPlan.riskLevel).type">
            {{ riskTag(currentPlan.riskLevel).label }}
          </ElTag>
        </div>

        <ElRadioGroup
          v-model="actionForm.actionType"
          class="action-tabs"
          @change="handleActionChange"
        >
          <ElRadioButton v-for="action in actionOptions" :key="action.value" :label="action.value">
            <ElIcon>
              <component :is="action.icon" />
            </ElIcon>
            <span>{{ action.shortLabel }}</span>
          </ElRadioButton>
        </ElRadioGroup>

        <ElAlert
          v-if="actionCapabilityWarning"
          class="capability-alert"
          type="warning"
          :title="actionCapabilityWarning"
          show-icon
          :closable="false"
        />

        <ElForm class="execution-form" label-position="top" :model="actionForm">
          <ElFormItem label="执行原因">
            <ElInput v-model="actionForm.reason" clearable />
          </ElFormItem>
          <ElFormItem v-if="showAddressInput" label="新收货地址">
            <ElInput v-model="actionForm.newAddress" clearable />
          </ElFormItem>
          <ElFormItem v-if="showRefundInput" label="退款金额">
            <ElInputNumber
              v-model="actionForm.refundAmount"
              :min="1"
              :max="selectedOrder?.refundableAmount || 500"
              :precision="2"
              :step="10"
              controls-position="right"
            />
          </ElFormItem>
          <ElFormItem v-if="showCouponInput" label="优惠补偿金额">
            <ElInputNumber
              v-model="actionForm.couponAmount"
              :min="1"
              :max="200"
              :precision="2"
              :step="10"
              controls-position="right"
            />
          </ElFormItem>
          <ElFormItem label="幂等键">
            <ElInput v-model="actionForm.idempotencyKey">
              <template #append>
                <ElButton :icon="Key" @click="resetIdempotencyKey" />
              </template>
            </ElInput>
          </ElFormItem>
          <ElFormItem label="人工确认">
            <ElSwitch
              v-model="actionForm.confirmRisk"
              inline-prompt
              active-text="已确认"
              inactive-text="走审批"
            />
          </ElFormItem>
          <ElFormItem label="失败注入">
            <ElSwitch
              v-model="actionForm.simulateAuditFailure"
              inline-prompt
              active-text="审计失败"
              inactive-text="关闭"
            />
          </ElFormItem>
        </ElForm>

        <div class="action-bar">
          <ElButton
            :icon="DocumentChecked"
            :loading="planLoading"
            :disabled="!selectedOrderNo"
            @click="handlePlan"
          >
            生成计划
          </ElButton>
          <ElButton
            type="primary"
            :icon="VideoPlay"
            :loading="executeLoading"
            :disabled="!selectedOrderNo"
            @click="handleExecute"
          >
            执行动作
          </ElButton>
          <ElButton
            type="warning"
            plain
            :icon="Refresh"
            :loading="executeLoading"
            :disabled="!latestExecution?.idempotencyKey"
            @click="handleReplay"
          >
            重放同键
          </ElButton>
        </div>

        <div class="plan-summary">
          <div class="summary-block">
            <span>计划摘要</span>
            <p>{{ currentPlan?.summary || '待生成' }}</p>
          </div>
          <div class="summary-grid">
            <div>
              <span>审批边界</span>
              <strong>{{ currentPlan?.humanBoundary || '-' }}</strong>
            </div>
            <div>
              <span>补偿方案</span>
              <strong>{{ currentPlan?.compensationPlan || '-' }}</strong>
            </div>
          </div>
        </div>

        <ElTable :data="visibleToolCalls" height="260" empty-text="暂无调用记录">
          <ElTableColumn label="步骤" min-width="144">
            <template #default="{ row }">
              <div class="tool-step">
                <strong>{{ row.stepName }}</strong>
                <span>{{ row.toolName }}</span>
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="输入" min-width="160" prop="inputSummary" show-overflow-tooltip />
          <ElTableColumn label="输出" min-width="180" prop="outputSummary" show-overflow-tooltip />
          <ElTableColumn label="状态" width="106">
            <template #default="{ row }">
              <ElTag size="small" :type="toolStatusTag(row.status).type">
                {{ toolStatusTag(row.status).label }}
              </ElTag>
            </template>
          </ElTableColumn>
        </ElTable>

        <div class="tool-registry">
          <div v-for="tool in visibleTools" :key="tool.toolName" class="tool-card">
            <ElIcon><Tools /></ElIcon>
            <div>
              <strong>{{ tool.title }}</strong>
              <span>{{ tool.toolName }}</span>
              <p>{{ tool.description }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="panel side-panel">
        <ElTabs v-model="sideTab" class="side-tabs">
          <ElTabPane name="approval">
            <template #label>
              <span class="tab-label">
                <ElIcon><Tickets /></ElIcon>
                审批
              </span>
            </template>
            <div v-if="pendingApprovals.length" class="approval-list">
              <div
                v-for="ticket in pendingApprovals"
                :key="ticket.approvalId"
                class="approval-item"
              >
                <div class="approval-head">
                  <strong>{{ ticket.orderNo }}</strong>
                  <ElTag size="small" :type="riskTag(ticket.riskLevel).type">
                    {{ riskTag(ticket.riskLevel).label }}
                  </ElTag>
                </div>
                <p>{{ ticket.reason }}</p>
                <span>{{ formatTime(ticket.requestedAt) }} · {{ ticket.requestedBy }}</span>
                <div class="approval-actions">
                  <ElButton
                    size="small"
                    type="success"
                    :icon="CircleCheck"
                    :loading="approvalLoading === ticket.approvalId"
                    @click="handleConfirmApproval(ticket)"
                  >
                    通过
                  </ElButton>
                  <ElButton
                    size="small"
                    :icon="CircleClose"
                    :loading="approvalLoading === ticket.approvalId"
                    @click="handleRejectApproval(ticket)"
                  >
                    拒绝
                  </ElButton>
                </div>
              </div>
            </div>
            <ElEmpty v-else description="暂无待审批动作" />
          </ElTabPane>

          <ElTabPane name="audit">
            <template #label>
              <span class="tab-label">
                <ElIcon><Memo /></ElIcon>
                审计
              </span>
            </template>
            <ElTimeline v-if="auditLogs.length" class="audit-timeline">
              <ElTimelineItem
                v-for="log in auditLogs"
                :key="log.auditId"
                :timestamp="formatTime(log.createdAt)"
                :type="auditTimelineType(log.eventType)"
                placement="top"
              >
                <div class="audit-item">
                  <strong>{{ auditEventLabel(log.eventType) }}</strong>
                  <span>{{ log.message }}</span>
                  <p>{{ log.detail }}</p>
                </div>
              </ElTimelineItem>
            </ElTimeline>
            <ElEmpty v-else description="暂无审计日志" />
          </ElTabPane>

          <ElTabPane name="compensation">
            <template #label>
              <span class="tab-label">
                <ElIcon><Operation /></ElIcon>
                补偿
              </span>
            </template>
            <div v-if="compensationTasks.length" class="compensation-list">
              <div
                v-for="task in compensationTasks"
                :key="task.compensationId"
                class="compensation-item"
              >
                <div class="approval-head">
                  <strong>{{ task.orderNo }}</strong>
                  <ElTag size="small" :type="compensationStatusTag(task.status).type">
                    {{ compensationStatusTag(task.status).label }}
                  </ElTag>
                </div>
                <p>{{ task.compensationAction }}</p>
                <span>{{ task.failedStep }} · {{ task.lastError || '无错误信息' }}</span>
                <ElButton
                  size="small"
                  type="primary"
                  plain
                  :icon="Refresh"
                  :disabled="task.status === 'SUCCEEDED'"
                  :loading="compensationLoading === task.compensationId"
                  @click="handleRetryCompensation(task)"
                >
                  重试补偿
                </ElButton>
              </div>
            </div>
            <ElEmpty v-else description="暂无补偿任务" />
          </ElTabPane>
        </ElTabs>

        <div v-if="latestExecution" class="latest-result">
          <div class="approval-head">
            <strong>最近执行</strong>
            <ElTag :type="executionStatusTag(latestExecution.status).type">
              {{ executionStatusTag(latestExecution.status).label }}
            </ElTag>
          </div>
          <p>{{ latestExecution.summary }}</p>
          <span>
            {{ latestExecution.executionId || latestExecution.approvalId }} ·
            {{ latestExecution.replay ? '幂等重放' : '首次执行' }}
          </span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, reactive, ref } from 'vue'
  import type { Component } from 'vue'
  import { ElMessage, ElMessageBox } from 'element-plus'
  import {
    Box,
    CircleCheck,
    CircleClose,
    Cpu,
    DocumentChecked,
    EditPen,
    Key,
    Location,
    Memo,
    Money,
    Operation,
    Promotion,
    Refresh,
    Search,
    Tickets,
    Tools,
    VideoPlay,
    Warning
  } from '@element-plus/icons-vue'
  import {
    confirmOrderOpsApproval,
    executeOrderOpsAction,
    fetchOrderOpsApprovals,
    fetchOrderOpsAuditLogs,
    fetchOrderOpsOrderDetail,
    fetchOrderOpsOrders,
    fetchOrderOpsTools,
    planOrderOpsAction,
    rejectOrderOpsApproval,
    retryOrderOpsCompensation
  } from '@/api/order-ops'
  import type {
    OrderOpsActionType,
    OrderOpsAgentPlan,
    OrderOpsApprovalTicket,
    OrderOpsAuditLog,
    OrderOpsAuditLogQuery,
    OrderOpsCompensationTask,
    OrderOpsExecuteParams,
    OrderOpsExecutionResult,
    OrderOpsOrder,
    OrderOpsOrderListQuery,
    OrderOpsOrderStatus,
    OrderOpsPlanParams,
    OrderOpsToolCall,
    OrderOpsToolDefinition
  } from '@/api/model/orderOpsModel'

  defineOptions({ name: 'OrderOpsConsole' })

  type TagType = 'success' | 'warning' | 'info' | 'primary' | 'danger'

  interface TagMeta {
    label: string
    type: TagType
  }

  interface ActionOption {
    value: OrderOpsActionType
    label: string
    shortLabel: string
    icon: Component
  }

  interface MetricItem {
    label: string
    value: string | number
    icon: Component
    tone: string
  }

  const operator = {
    id: 'ops-teacher',
    name: '运营讲师'
  }

  const actionOptions: ActionOption[] = [
    { value: 'QUERY_ORDER', label: '订单查询', shortLabel: '查询', icon: Search },
    { value: 'INTERCEPT_SHIPMENT', label: '发货拦截', shortLabel: '拦截', icon: Box },
    { value: 'CHANGE_ADDRESS', label: '地址修改', shortLabel: '改址', icon: EditPen },
    { value: 'APPLY_REFUND', label: '退款申请', shortLabel: '退款', icon: Money },
    { value: 'ISSUE_COUPON', label: '优惠补偿', shortLabel: '补偿', icon: Promotion }
  ]

  const orderStatusOptions: Array<{ label: string; value: OrderOpsOrderStatus }> = [
    { label: '已创建', value: 'CREATED' },
    { label: '已支付', value: 'PAID' },
    { label: '履约中', value: 'FULFILLING' },
    { label: '已发货', value: 'SHIPPED' },
    { label: '已签收', value: 'DELIVERED' },
    { label: '已关闭', value: 'CLOSED' },
    { label: '已取消', value: 'CANCELLED' }
  ]

  const searchForm = reactive<{
    keyword: string
    orderStatus: OrderOpsOrderStatus | ''
  }>({
    keyword: '',
    orderStatus: ''
  })

  const actionForm = reactive({
    actionType: 'INTERCEPT_SHIPMENT' as OrderOpsActionType,
    reason: '客户在未出库前提交运营工单',
    newAddress: '上海市浦东新区张江路 88 号 5 栋 1201',
    refundAmount: 50,
    couponAmount: 20,
    idempotencyKey: '',
    confirmRisk: false,
    simulateAuditFailure: false
  })

  const orders = ref<OrderOpsOrder[]>([])
  const tools = ref<OrderOpsToolDefinition[]>([])
  const approvals = ref<OrderOpsApprovalTicket[]>([])
  const auditLogs = ref<OrderOpsAuditLog[]>([])
  const compensationTasks = ref<OrderOpsCompensationTask[]>([])
  const orderDetail = ref<OrderOpsOrder | null>(null)
  const currentPlan = ref<OrderOpsAgentPlan | null>(null)
  const latestExecution = ref<OrderOpsExecutionResult | null>(null)
  const selectedOrderNo = ref('')
  const sideTab = ref('approval')
  const pageLoading = ref(false)
  const orderLoading = ref(false)
  const planLoading = ref(false)
  const executeLoading = ref(false)
  const approvalLoading = ref('')
  const compensationLoading = ref('')

  const selectedOrder = computed(() => {
    return orderDetail.value || orders.value.find((item) => item.orderNo === selectedOrderNo.value)
  })

  const showAddressInput = computed(() => actionForm.actionType === 'CHANGE_ADDRESS')
  const showRefundInput = computed(() => actionForm.actionType === 'APPLY_REFUND')
  const showCouponInput = computed(() => actionForm.actionType === 'ISSUE_COUPON')

  const pendingApprovals = computed(() => {
    return approvals.value.filter((item) => item.status === 'PENDING')
  })

  const visibleToolCalls = computed<OrderOpsToolCall[]>(() => {
    return latestExecution.value?.toolCalls?.length
      ? latestExecution.value.toolCalls
      : currentPlan.value?.toolCalls || []
  })

  const visibleTools = computed(() => {
    return tools.value.filter((tool) => {
      return tool.actionType === actionForm.actionType || tool.actionType === 'QUERY_ORDER'
    })
  })

  const metrics = computed<MetricItem[]>(() => [
    { label: '样例订单', value: orders.value.length, icon: Box, tone: 'blue' },
    { label: '待审批', value: pendingApprovals.value.length, icon: Tickets, tone: 'amber' },
    {
      label: '补偿任务',
      value: compensationTasks.value.filter((item) => item.status !== 'SUCCEEDED').length,
      icon: Operation,
      tone: 'teal'
    },
    { label: '审计日志', value: auditLogs.value.length, icon: Memo, tone: 'gray' }
  ])

  const actionCapabilityWarning = computed(() => {
    const order = selectedOrder.value
    if (!order) return ''
    if (actionForm.actionType === 'INTERCEPT_SHIPMENT' && !order.canIntercept) {
      return '当前订单不满足发货拦截条件'
    }
    if (actionForm.actionType === 'CHANGE_ADDRESS' && !order.canChangeAddress) {
      return '当前订单不满足地址修改条件'
    }
    if (actionForm.actionType === 'APPLY_REFUND' && !order.canRefund) {
      return '当前订单不满足退款申请条件'
    }
    if (actionForm.actionType === 'ISSUE_COUPON' && !order.canCompensate) {
      return '当前订单不满足优惠补偿条件'
    }
    return ''
  })

  const orderStatusMap: Record<string, TagMeta> = {
    CREATED: { label: '已创建', type: 'info' },
    PAID: { label: '已支付', type: 'success' },
    FULFILLING: { label: '履约中', type: 'primary' },
    SHIPPED: { label: '已发货', type: 'warning' },
    DELIVERED: { label: '已签收', type: 'success' },
    CLOSED: { label: '已关闭', type: 'info' },
    CANCELLED: { label: '已取消', type: 'danger' }
  }

  const shipmentStatusMap: Record<string, TagMeta> = {
    NOT_CREATED: { label: '未建单', type: 'info' },
    PENDING: { label: '待履约', type: 'info' },
    ALLOCATED: { label: '已分仓', type: 'primary' },
    INTERCEPTED: { label: '已拦截', type: 'warning' },
    IN_TRANSIT: { label: '运输中', type: 'warning' },
    DELIVERED: { label: '已签收', type: 'success' },
    RETURNING: { label: '退回中', type: 'danger' }
  }

  const toolStatusMap: Record<string, TagMeta> = {
    PENDING: { label: '待执行', type: 'info' },
    RUNNING: { label: '执行中', type: 'primary' },
    SUCCESS: { label: '成功', type: 'success' },
    FAILED: { label: '失败', type: 'danger' },
    SKIPPED: { label: '跳过', type: 'warning' }
  }

  const executionStatusMap: Record<string, TagMeta> = {
    WAITING_APPROVAL: { label: '待审批', type: 'warning' },
    SUCCESS: { label: '成功', type: 'success' },
    FAILED: { label: '失败', type: 'danger' }
  }

  const compensationStatusMap: Record<string, TagMeta> = {
    PENDING: { label: '待补偿', type: 'warning' },
    SUCCEEDED: { label: '已补偿', type: 'success' },
    FAILED: { label: '补偿失败', type: 'danger' }
  }

  const riskMap: Record<string, TagMeta> = {
    LOW: { label: '低风险', type: 'success' },
    MEDIUM: { label: '中风险', type: 'warning' },
    HIGH: { label: '高风险', type: 'danger' }
  }

  const auditEventMap: Record<string, string> = {
    ORDER_QUERIED: '订单查询',
    PLAN_CREATED: '计划生成',
    IDEMPOTENCY_REPLAYED: '幂等重放',
    APPROVAL_CREATED: '审批创建',
    APPROVAL_CONFIRMED: '审批通过',
    APPROVAL_REJECTED: '审批拒绝',
    TOOL_CALL_STARTED: '工具开始',
    TOOL_CALL_SUCCEEDED: '工具成功',
    TOOL_CALL_FAILED: '工具失败',
    EXECUTION_SUCCEEDED: '执行成功',
    EXECUTION_FAILED: '执行失败',
    COMPENSATION_CREATED: '补偿创建',
    COMPENSATION_RETRIED: '补偿重试',
    COMPENSATION_SUCCEEDED: '补偿成功',
    COMPENSATION_FAILED: '补偿失败'
  }

  const createIdempotencyKey = () => {
    return `ops-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
  }

  const resetIdempotencyKey = () => {
    actionForm.idempotencyKey = createIdempotencyKey()
  }

  const actionLabel = (actionType?: string) => {
    return actionOptions.find((item) => item.value === actionType)?.label || actionType || '-'
  }

  const getTag = (map: Record<string, TagMeta>, value?: string): TagMeta => {
    return map[value || ''] || { label: value || '-', type: 'info' }
  }

  const orderStatusTag = (status?: string) => getTag(orderStatusMap, status)
  const shipmentStatusTag = (status?: string) => getTag(shipmentStatusMap, status)
  const toolStatusTag = (status?: string) => getTag(toolStatusMap, status)
  const executionStatusTag = (status?: string) => getTag(executionStatusMap, status)
  const compensationStatusTag = (status?: string) => getTag(compensationStatusMap, status)
  const riskTag = (risk?: string) => getTag(riskMap, risk)

  const auditEventLabel = (eventType?: string) => {
    return auditEventMap[eventType || ''] || eventType || '审计事件'
  }

  const auditTimelineType = (eventType?: string): TagType => {
    if (!eventType) return 'info'
    if (eventType.includes('FAILED') || eventType.includes('REJECTED')) return 'danger'
    if (eventType.includes('CREATED') || eventType.includes('REPLAYED')) return 'warning'
    if (eventType.includes('SUCCEEDED') || eventType.includes('CONFIRMED')) return 'success'
    return 'primary'
  }

  const formatMoney = (value?: number) => {
    return typeof value === 'number' ? `¥${value.toFixed(2)}` : '¥0.00'
  }

  const formatTime = (value?: string) => {
    return value ? value.replace('T', ' ').slice(0, 19) : '-'
  }

  const orderRowClassName = ({ row }: { row: OrderOpsOrder }) => {
    return row.orderNo === selectedOrderNo.value ? 'is-selected-order' : ''
  }

  const buildBaseParams = (): OrderOpsPlanParams => {
    if (!selectedOrderNo.value) {
      throw new Error('请选择订单')
    }

    return {
      orderNo: selectedOrderNo.value,
      actionType: actionForm.actionType,
      operatorId: operator.id,
      operatorName: operator.name,
      reason: actionForm.reason,
      newAddress: showAddressInput.value ? actionForm.newAddress : undefined,
      refundAmount: showRefundInput.value ? actionForm.refundAmount : undefined,
      couponAmount: showCouponInput.value ? actionForm.couponAmount : undefined
    }
  }

  const buildExecuteParams = (): OrderOpsExecuteParams => {
    return {
      ...buildBaseParams(),
      idempotencyKey: actionForm.idempotencyKey,
      confirmRisk: actionForm.confirmRisk,
      simulateFailureStage: actionForm.simulateAuditFailure ? 'write_operation_audit' : undefined
    }
  }

  const syncCompensationTask = (task?: OrderOpsCompensationTask) => {
    if (!task?.compensationId) return
    const index = compensationTasks.value.findIndex(
      (item) => item.compensationId === task.compensationId
    )
    if (index >= 0) {
      compensationTasks.value[index] = task
    } else {
      compensationTasks.value.unshift(task)
    }
  }

  const loadOrders = async () => {
    orderLoading.value = true
    try {
      const query: OrderOpsOrderListQuery = {
        keyword: searchForm.keyword || undefined,
        orderStatus: searchForm.orderStatus || undefined
      }
      orders.value = await fetchOrderOpsOrders(query)
      const currentExists = orders.value.some((item) => item.orderNo === selectedOrderNo.value)
      const nextOrderNo = currentExists ? selectedOrderNo.value : orders.value[0]?.orderNo || ''
      if (nextOrderNo) {
        await selectOrder(nextOrderNo, { resetFlow: false })
      } else {
        selectedOrderNo.value = ''
        orderDetail.value = null
      }
    } finally {
      orderLoading.value = false
    }
  }

  const loadTools = async () => {
    tools.value = await fetchOrderOpsTools()
  }

  const loadApprovals = async () => {
    approvals.value = await fetchOrderOpsApprovals()
  }

  const loadAuditLogs = async () => {
    const query: OrderOpsAuditLogQuery = {
      orderNo: selectedOrderNo.value || undefined,
      limit: 30
    }
    auditLogs.value = await fetchOrderOpsAuditLogs(query)
  }

  const loadAll = async () => {
    pageLoading.value = true
    try {
      await Promise.all([loadTools(), loadApprovals()])
      await loadOrders()
      await loadAuditLogs()
    } finally {
      pageLoading.value = false
    }
  }

  const selectOrder = async (orderNo: string, options: { resetFlow?: boolean } = {}) => {
    const changed = selectedOrderNo.value !== orderNo
    const shouldResetFlow = options.resetFlow ?? true
    selectedOrderNo.value = orderNo
    orderDetail.value = await fetchOrderOpsOrderDetail(orderNo)
    if (shouldResetFlow || changed) {
      currentPlan.value = null
      latestExecution.value = null
    }
    await loadAuditLogs()
  }

  const handleOrderRowClick = (row: OrderOpsOrder) => {
    if (row.orderNo) {
      selectOrder(row.orderNo)
    }
  }

  const handleActionChange = () => {
    currentPlan.value = null
    latestExecution.value = null
    resetIdempotencyKey()
  }

  const handlePlan = async () => {
    planLoading.value = true
    try {
      currentPlan.value = await planOrderOpsAction(buildBaseParams())
      latestExecution.value = null
      await loadAuditLogs()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '计划生成失败')
    } finally {
      planLoading.value = false
    }
  }

  const handleExecute = async () => {
    executeLoading.value = true
    try {
      const result = await executeOrderOpsAction(buildExecuteParams())
      latestExecution.value = result
      syncCompensationTask(result.compensationTask)
      if (result.status === 'WAITING_APPROVAL') {
        sideTab.value = 'approval'
        ElMessage.warning('高风险动作已进入人工审批')
      }
      await Promise.all([loadOrders(), loadApprovals(), loadAuditLogs()])
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '执行失败')
    } finally {
      executeLoading.value = false
    }
  }

  const handleReplay = async () => {
    if (latestExecution.value?.idempotencyKey) {
      actionForm.idempotencyKey = latestExecution.value.idempotencyKey
    }
    await handleExecute()
  }

  const handleConfirmApproval = async (ticket: OrderOpsApprovalTicket) => {
    if (!ticket.approvalId) return
    approvalLoading.value = ticket.approvalId
    try {
      const approved = await confirmOrderOpsApproval(ticket.approvalId, {
        operatorId: operator.id,
        operatorName: operator.name,
        comment: '人工复核通过'
      })
      approvals.value = approvals.value.map((item) =>
        item.approvalId === approved.approvalId ? approved : item
      )
      if (approved.result) {
        latestExecution.value = approved.result
        syncCompensationTask(approved.result.compensationTask)
      }
      await Promise.all([loadOrders(), loadApprovals(), loadAuditLogs()])
    } finally {
      approvalLoading.value = ''
    }
  }

  const handleRejectApproval = async (ticket: OrderOpsApprovalTicket) => {
    if (!ticket.approvalId) return
    const confirmed = await ElMessageBox.confirm('确认拒绝该高风险动作？', '审批确认', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning'
    }).catch(() => false)
    if (!confirmed) return
    approvalLoading.value = ticket.approvalId
    try {
      const rejected = await rejectOrderOpsApproval(ticket.approvalId, {
        operatorId: operator.id,
        operatorName: operator.name,
        comment: '人工复核拒绝'
      })
      approvals.value = approvals.value.map((item) =>
        item.approvalId === rejected.approvalId ? rejected : item
      )
      await Promise.all([loadApprovals(), loadAuditLogs()])
    } finally {
      approvalLoading.value = ''
    }
  }

  const handleRetryCompensation = async (task: OrderOpsCompensationTask) => {
    if (!task.compensationId) return
    compensationLoading.value = task.compensationId
    try {
      const nextTask = await retryOrderOpsCompensation(task.compensationId, {
        operatorId: operator.id,
        operatorName: operator.name,
        comment: '运营侧重试补偿'
      })
      syncCompensationTask(nextTask)
      await Promise.all([loadOrders(), loadAuditLogs()])
    } finally {
      compensationLoading.value = ''
    }
  }

  resetIdempotencyKey()

  onMounted(() => {
    loadAll()
  })
</script>

<style scoped lang="scss">
  .order-ops-page {
    display: flex;
    flex-direction: column;
    gap: 16px;
    min-height: calc(100vh - 96px);
    color: var(--el-text-color-primary);
  }

  .ops-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    padding: 24px 28px;
    overflow: hidden;
    background:
      linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(242, 248, 247, 0.94)),
      linear-gradient(90deg, rgba(14, 116, 144, 0.18), rgba(245, 158, 11, 0.14));
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;
  }

  .ops-header h1 {
    margin: 2px 0 12px;
    font-size: 28px;
    font-weight: 700;
    line-height: 1.2;
    letter-spacing: 0;
  }

  .ops-eyebrow {
    margin: 0;
    font-size: 12px;
    font-weight: 700;
    color: #0f766e;
    text-transform: uppercase;
  }

  .header-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .metrics-strip {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  .metric-tile {
    display: flex;
    align-items: center;
    gap: 12px;
    min-height: 76px;
    padding: 16px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;
  }

  .metric-tile span,
  .summary-block span,
  .summary-grid span,
  .approval-item span,
  .compensation-item span,
  .latest-result span,
  .order-main span,
  .tool-step span,
  .tool-card span {
    display: block;
    font-size: 12px;
    line-height: 1.45;
    color: var(--el-text-color-secondary);
  }

  .metric-tile strong {
    display: block;
    margin-top: 4px;
    font-size: 24px;
    line-height: 1;
  }

  .metric-icon {
    width: 38px;
    height: 38px;
    font-size: 20px;
    border-radius: 8px;
  }

  .metric-icon.blue {
    color: #0369a1;
    background: #e0f2fe;
  }

  .metric-icon.amber {
    color: #b45309;
    background: #fef3c7;
  }

  .metric-icon.teal {
    color: #0f766e;
    background: #ccfbf1;
  }

  .metric-icon.gray {
    color: #475569;
    background: #e2e8f0;
  }

  .ops-grid {
    display: grid;
    grid-template-columns: minmax(330px, 0.95fr) minmax(460px, 1.3fr) minmax(340px, 0.95fr);
    gap: 16px;
    align-items: start;
  }

  .panel {
    min-width: 0;
    padding: 18px;
    background: var(--el-bg-color);
    border: 1px solid var(--el-border-color-light);
    border-radius: 8px;
  }

  .panel-heading,
  .approval-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .panel-heading {
    margin-bottom: 16px;
  }

  .panel-heading p {
    margin: 0 0 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .panel-heading strong {
    font-size: 18px;
    line-height: 1.25;
    word-break: break-word;
  }

  .search-form {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 116px auto;
    gap: 8px;
    margin-bottom: 12px;
  }

  .search-form :deep(.el-form-item) {
    margin: 0;
  }

  .status-filter {
    width: 116px;
  }

  .order-main strong,
  .tool-step strong,
  .tool-card strong {
    display: block;
    line-height: 1.45;
    word-break: break-word;
  }

  .order-detail {
    padding-top: 16px;
  }

  .detail-line {
    display: flex;
    gap: 8px;
    align-items: flex-start;
    margin-bottom: 12px;
    font-size: 13px;
    line-height: 1.5;
  }

  .capability-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 10px;
  }

  .order-timeline,
  .audit-timeline {
    margin-top: 8px;
  }

  .order-timeline :deep(.el-timeline-item__content) {
    display: grid;
    gap: 3px;
    font-size: 12px;
  }

  .action-tabs {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    width: 100%;
    margin-bottom: 14px;
  }

  .action-tabs :deep(.el-radio-button__inner) {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    width: 100%;
    min-height: 40px;
    padding: 8px;
    white-space: normal;
  }

  .capability-alert {
    margin-bottom: 12px;
  }

  .execution-form {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px 12px;
  }

  .execution-form :deep(.el-form-item) {
    margin-bottom: 0;
  }

  .execution-form :deep(.el-input-number) {
    width: 100%;
  }

  .action-bar {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin: 16px 0;
  }

  .plan-summary {
    display: grid;
    gap: 10px;
    margin-bottom: 14px;
  }

  .summary-block,
  .summary-grid > div {
    padding: 12px;
    background: var(--el-fill-color-lighter);
    border: 1px solid var(--el-border-color-extra-light);
    border-radius: 8px;
  }

  .summary-block p,
  .approval-item p,
  .compensation-item p,
  .latest-result p,
  .audit-item p,
  .tool-card p {
    margin: 6px 0 0;
    font-size: 13px;
    line-height: 1.5;
    color: var(--el-text-color-regular);
    word-break: break-word;
  }

  .summary-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .summary-grid strong {
    display: block;
    margin-top: 6px;
    font-size: 13px;
    line-height: 1.5;
    word-break: break-word;
  }

  .tool-registry {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    margin-top: 14px;
  }

  .tool-card {
    display: grid;
    grid-template-columns: 24px minmax(0, 1fr);
    gap: 10px;
    padding: 12px;
    border: 1px solid var(--el-border-color-extra-light);
    border-radius: 8px;
  }

  .tool-card .el-icon {
    margin-top: 2px;
    color: #0f766e;
  }

  .side-tabs :deep(.el-tabs__content) {
    min-height: 470px;
  }

  .tab-label {
    display: inline-flex;
    gap: 6px;
    align-items: center;
  }

  .approval-list,
  .compensation-list {
    display: grid;
    gap: 12px;
  }

  .approval-item,
  .compensation-item,
  .latest-result {
    padding: 13px;
    border: 1px solid var(--el-border-color-extra-light);
    border-radius: 8px;
  }

  .approval-actions {
    display: flex;
    gap: 8px;
    margin-top: 12px;
  }

  .audit-item {
    display: grid;
    gap: 4px;
  }

  .audit-item strong {
    font-size: 13px;
  }

  .audit-item span {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .compensation-item .el-button {
    margin-top: 12px;
  }

  .latest-result {
    margin-top: 14px;
    background: var(--el-fill-color-lighter);
  }

  :deep(.is-selected-order td) {
    background: rgba(14, 165, 164, 0.08) !important;
  }

  @media (max-width: 1280px) {
    .ops-grid {
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    }

    .side-panel {
      grid-column: 1 / -1;
    }
  }

  @media (max-width: 900px) {
    .ops-header {
      align-items: flex-start;
      padding: 20px;
    }

    .metrics-strip,
    .ops-grid,
    .execution-form,
    .summary-grid,
    .tool-registry {
      grid-template-columns: 1fr;
    }

    .search-form {
      grid-template-columns: 1fr;
    }

    .status-filter {
      width: 100%;
    }

    .action-tabs {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 560px) {
    .ops-header {
      flex-direction: column;
    }

    .ops-header h1 {
      font-size: 23px;
    }

    .action-tabs {
      grid-template-columns: 1fr;
    }
  }
</style>
