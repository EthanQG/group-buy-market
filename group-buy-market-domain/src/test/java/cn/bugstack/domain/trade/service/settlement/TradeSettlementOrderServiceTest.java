package cn.bugstack.domain.trade.service.settlement;

import cn.bugstack.domain.trade.adapter.port.ITradePort;
import cn.bugstack.domain.trade.adapter.repository.ITradeRepository;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.bugstack.domain.trade.model.entity.*;
import cn.bugstack.domain.trade.model.valobj.NotifyConfigVO;
import cn.bugstack.domain.trade.model.valobj.NotifyTypeEnumVO;
import cn.bugstack.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import cn.bugstack.types.design.framework.link.model2.chain.BusinessLinkedList;
import cn.bugstack.types.enums.GroupBuyOrderEnumVO;
import cn.bugstack.types.enums.NotifyTaskHTTPEnumVO;
import cn.bugstack.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TradeSettlementOrderServiceTest {

    @Mock
    private ITradeRepository repository;

    @Mock
    private ITradePort port;

    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    @Mock
    private BusinessLinkedList<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> tradeSettlementRuleFilter;

    @InjectMocks
    private TradeSettlementOrderService settlementService;

    private GroupBuyTeamSettlementAggregate settleAggregate;
    private TradePaySuccessEntity tradePaySuccessEntity;

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(settlementService, "threadPoolExecutor", threadPoolExecutor);

        UserEntity userEntity = UserEntity.builder().userId("user_001").build();

        GroupBuyTeamEntity groupBuyTeamEntity = GroupBuyTeamEntity.builder()
                .teamId("TEAM123")
                .activityId(1001L)
                .targetCount(3)
                .completeCount(1)
                .lockCount(2)
                .status(GroupBuyOrderEnumVO.PROGRESS)
                .validStartTime(new Date(System.currentTimeMillis() - 86400000))
                .validEndTime(new Date(System.currentTimeMillis() + 86400000))
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(NotifyTypeEnumVO.HTTP)
                        .notifyUrl("http://example.com/notify")
                        .notifyMQ("topic.team.success")
                        .build())
                .build();

        tradePaySuccessEntity = TradePaySuccessEntity.builder()
                .userId("user_001")
                .outTradeNo("OUT_TRADE_123")
                .outTradeTime(new Date())
                .source("WEB")
                .channel("ALIPAY")
                .build();

        settleAggregate = GroupBuyTeamSettlementAggregate.builder()
                .userEntity(userEntity)
                .groupBuyTeamEntity(groupBuyTeamEntity)
                .tradePaySuccessEntity(tradePaySuccessEntity)
                .build();

        // Mock tradeSettlementRuleFilter 返回值
        TradeSettlementRuleFilterBackEntity filterBack = TradeSettlementRuleFilterBackEntity.builder()
                .teamId("TEAM123")
                .activityId(1001L)
                .targetCount(3)
                .completeCount(1)
                .lockCount(2)
                .status(GroupBuyOrderEnumVO.PROGRESS)
                .validStartTime(new Date(System.currentTimeMillis() - 86400000))
                .validEndTime(new Date(System.currentTimeMillis() + 86400000))
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(NotifyTypeEnumVO.HTTP)
                        .notifyUrl("http://example.com/notify")
                        .notifyMQ("topic.team.success")
                        .build())
                .build();
        when(tradeSettlementRuleFilter.apply(any(), any())).thenReturn(filterBack);
    }

    @Test
    public void test_settlement_last_order_completes_team() throws Exception {
        // 设置满员团队的过滤器返回值
        TradeSettlementRuleFilterBackEntity fullFilterBack = TradeSettlementRuleFilterBackEntity.builder()
                .teamId("TEAM123")
                .activityId(1001L)
                .targetCount(3)//拼团人数为3人
                .completeCount(2)//已完成人数为2人
                .lockCount(3)//已锁定人数为3人
                .status(GroupBuyOrderEnumVO.PROGRESS)
                .validStartTime(new Date(System.currentTimeMillis() - 86400000))
                .validEndTime(new Date(System.currentTimeMillis() + 86400000))
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(NotifyTypeEnumVO.HTTP)
                        .notifyUrl("http://example.com/notify")
                        .notifyMQ("topic.team.success")
                        .build())
                .build();
        when(tradeSettlementRuleFilter.apply(any(), any())).thenReturn(fullFilterBack);

        // Mock结算返回通知任务
        NotifyTaskEntity mockNotifyTask = NotifyTaskEntity.builder()
                .teamId("TEAM123")
                .notifyType(NotifyTypeEnumVO.HTTP.getCode())
                .notifyUrl("http://example.com/notify")
                .notifyCount(0)
                .parameterJson("{}")
                .build();

        when(repository.settlementMarketPayOrder(any())).thenReturn(mockNotifyTask);
        //Mock线程池执行（非阻塞）
        doNothing().when(threadPoolExecutor).execute(any(Runnable.class));

        TradePaySettlementEntity result = settlementService.settlementMarketPayOrder(tradePaySuccessEntity);

        assertNotNull(result);
        assertEquals("TEAM123", result.getTeamId());
        assertEquals("user_001", result.getUserId());
    }

    //非最后一单不通知,用户不是拼团的最后一单
    @Test
    public void test_settlement_intermediate_order_no_notify() throws Exception {
        GroupBuyTeamEntity middleTeamEntity = GroupBuyTeamEntity.builder()
                .teamId("TEAM123")
                .activityId(1001L)
                .targetCount(5)
                .completeCount(2)
                .lockCount(3)
                .status(GroupBuyOrderEnumVO.PROGRESS)
                .validStartTime(new Date(System.currentTimeMillis() - 86400000))
                .validEndTime(new Date(System.currentTimeMillis() + 86400000))
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(NotifyTypeEnumVO.HTTP)
                        .notifyUrl("http://example.com/notify")
                        .build())
                .build();

        settleAggregate = GroupBuyTeamSettlementAggregate.builder()
                .userEntity(settleAggregate.getUserEntity())
                .groupBuyTeamEntity(middleTeamEntity)
                .tradePaySuccessEntity(tradePaySuccessEntity)
                .build();

        // Mock结算返回null（未满人数，不需要通知）
        when(repository.settlementMarketPayOrder(any())).thenReturn(null);

        TradePaySettlementEntity result = settlementService.settlementMarketPayOrder(tradePaySuccessEntity);

        // 验证：非满员时不需要通知，但teamId仍然存在
        assertNotNull(result);
        assertNotNull(result.getTeamId());
        assertEquals("TEAM123", result.getTeamId());
        
        // 验证：回调处理没有执行（线程池没有被调用）
        verify(threadPoolExecutor, never()).execute(any(Runnable.class));
    }

    // 测试结算订单状态更新失败
    @Test
    public void test_settlement_order_status_update_failure() {
        //Mock结算抛出乐观锁异常
        when(repository.settlementMarketPayOrder(any())).thenThrow(new AppException(cn.bugstack.types.enums.ResponseCode.UPDATE_ZERO));

        try {
            settlementService.settlementMarketPayOrder(tradePaySuccessEntity);
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(cn.bugstack.types.enums.ResponseCode.UPDATE_ZERO.getCode(), e.getCode());
        } catch (Exception e) {
            fail("Expected AppException");
        }
    }

    //定时任务通知下游系统成功
    @Test
    public void test_settlement_notify_success() throws Exception {
        //构造待通知任务列表
        List<NotifyTaskEntity> notifyTasks = new ArrayList<>();
        notifyTasks.add(NotifyTaskEntity.builder()
                .teamId("TEAM123")
                .notifyUrl("http://example.com/notify")
                .notifyCount(0)
                .notifyType(NotifyTypeEnumVO.HTTP.getCode())
                .parameterJson("{}")
                .build());

        when(repository.queryUnExecutedNotifyTaskList()).thenReturn(notifyTasks);
        when(port.groupBuyNotify(any())).thenReturn(NotifyTaskHTTPEnumVO.SUCCESS.getCode());
        when(repository.updateNotifyTaskStatusSuccess(anyString())).thenReturn(1);

        java.util.Map<String, Integer> result = settlementService.execSettlementNotifyJob();

        assertNotNull(result);
        assertEquals(1, result.get("successCount").intValue());
        assertEquals(0, result.get("errorCount").intValue());
    }

    //定时任务通知下游系统失败，重试
    @Test
    public void test_settlement_notify_failure_with_retry() throws Exception {
        List<NotifyTaskEntity> notifyTasks = new ArrayList<>();
        notifyTasks.add(NotifyTaskEntity.builder()
                .teamId("TEAM123")
                .notifyUrl("http://example.com/notify")
                .notifyCount(4)//已重试4次
                .notifyType(NotifyTypeEnumVO.HTTP.getCode())
                .parameterJson("{}")
                .build());

        when(repository.queryUnExecutedNotifyTaskList()).thenReturn(notifyTasks);
        when(port.groupBuyNotify(any())).thenReturn(NotifyTaskHTTPEnumVO.ERROR.getCode());
        when(repository.updateNotifyTaskStatusError(anyString())).thenReturn(1);

        java.util.Map<String, Integer> result = settlementService.execSettlementNotifyJob();

        assertNotNull(result);
        assertEquals(1, result.get("errorCount").intValue());
        assertEquals(0, result.get("retryCount").intValue());
    }

    //定时任务通知下游系统失败，重试次数超过最大重试次数
    @Test
    public void test_settlement_notify_retry_exhausted() throws Exception {
        List<NotifyTaskEntity> notifyTasks = new ArrayList<>();
        notifyTasks.add(NotifyTaskEntity.builder()
                .teamId("TEAM123")
                .notifyUrl("http://example.com/notify")
                .notifyCount(5)//已重试5次
                .notifyType(NotifyTypeEnumVO.HTTP.getCode())
                .parameterJson("{}")
                .build());

        when(repository.queryUnExecutedNotifyTaskList()).thenReturn(notifyTasks);
        when(port.groupBuyNotify(any())).thenReturn(NotifyTaskHTTPEnumVO.ERROR.getCode());
        when(repository.updateNotifyTaskStatusRetry(anyString())).thenReturn(1);

        java.util.Map<String, Integer> result = settlementService.execSettlementNotifyJob();

        assertNotNull(result);
        // 验证：重试次数增加1次
        assertEquals(1, result.get("retryCount").intValue());
        assertEquals(0, result.get("errorCount").intValue());
    }

    //使用MQ消息队列通知
    @Test
    public void test_settlement_mq_notify() throws Exception {
        List<NotifyTaskEntity> notifyTasks = new ArrayList<>();
        notifyTasks.add(NotifyTaskEntity.builder()
                .teamId("TEAM123")
                .notifyMQ("topic.team.success")
                .notifyCount(0)
                .notifyType(NotifyTypeEnumVO.MQ.getCode())//MQ通知
                .parameterJson("{}")
                .build());

        when(repository.queryUnExecutedNotifyTaskList()).thenReturn(notifyTasks);
        when(port.groupBuyNotify(any())).thenReturn(NotifyTaskHTTPEnumVO.SUCCESS.getCode());
        when(repository.updateNotifyTaskStatusSuccess(anyString())).thenReturn(1);

        java.util.Map<String, Integer> result = settlementService.execSettlementNotifyJob();

        assertNotNull(result);
        assertEquals(1, result.get("successCount").intValue());
    }

    //空任务列表
    @Test
    public void test_settlement_empty_notify_list() throws Exception {
        when(repository.queryUnExecutedNotifyTaskList()).thenReturn(new ArrayList<>());

        java.util.Map<String, Integer> result = settlementService.execSettlementNotifyJob();

        assertNotNull(result);
        assertEquals(0, result.get("waitCount").intValue());
        assertEquals(0, result.get("successCount").intValue());
    }

    // DT-003: 重复通知测试 - 验证幂等性
    @Test
    public void test_settlement_notify_duplicate() throws Exception {
        List<NotifyTaskEntity> notifyTasks = new ArrayList<>();
        notifyTasks.add(NotifyTaskEntity.builder()
                .teamId("TEAM123")
                .notifyUrl("http://example.com/notify")
                .notifyCount(0)
                .notifyType(NotifyTypeEnumVO.HTTP.getCode())
                .parameterJson("{}")
                .build());

        when(repository.queryUnExecutedNotifyTaskList()).thenReturn(notifyTasks);
        when(port.groupBuyNotify(any())).thenReturn(NotifyTaskHTTPEnumVO.SUCCESS.getCode());
        // 第一次更新成功，第二次更新返回0（幂等处理）
        when(repository.updateNotifyTaskStatusSuccess(anyString())).thenReturn(1).thenReturn(0);

        // 第一次执行
        java.util.Map<String, Integer> result1 = settlementService.execSettlementNotifyJob();
        assertEquals(1, result1.get("successCount").intValue());

        // 第二次执行（重复通知）
        java.util.Map<String, Integer> result2 = settlementService.execSettlementNotifyJob();
        assertEquals(0, result2.get("successCount").intValue());  // 第二次更新0条，幂等处理
    }

    // DT-005: 网络抖动测试 - 模拟随机超时
    @Test
    public void test_settlement_notify_network_fluctuation() throws Exception {
        List<NotifyTaskEntity> notifyTasks = new ArrayList<>();
        notifyTasks.add(NotifyTaskEntity.builder()
                .teamId("TEAM123")
                .notifyUrl("http://example.com/notify")
                .notifyCount(3)
                .notifyType(NotifyTypeEnumVO.HTTP.getCode())
                .parameterJson("{}")
                .build());

        when(repository.queryUnExecutedNotifyTaskList()).thenReturn(notifyTasks);
        // 模拟网络抖动，第一次失败，第二次成功
        when(port.groupBuyNotify(any()))
                .thenReturn(NotifyTaskHTTPEnumVO.ERROR.getCode())
                .thenReturn(NotifyTaskHTTPEnumVO.SUCCESS.getCode());
        when(repository.updateNotifyTaskStatusError(anyString())).thenReturn(1);
        when(repository.updateNotifyTaskStatusSuccess(anyString())).thenReturn(1);

        // 第一次执行（失败）
        java.util.Map<String, Integer> result1 = settlementService.execSettlementNotifyJob();
        assertEquals(1, result1.get("errorCount").intValue());

        // 第二次执行（成功）
        java.util.Map<String, Integer> result2 = settlementService.execSettlementNotifyJob();
        assertEquals(1, result2.get("successCount").intValue());
    }

    // DT-006: 消息顺序错乱测试 - 验证顺序无关性
    @Test
    public void test_settlement_notify_order_disorder() throws Exception {
        List<NotifyTaskEntity> notifyTasks = new ArrayList<>();
        // 模拟顺序错乱：TEAM200 先执行，TEAM100 后执行
        notifyTasks.add(NotifyTaskEntity.builder()
                .teamId("TEAM200")
                .notifyUrl("http://example.com/notify")
                .notifyCount(0)
                .notifyType(NotifyTypeEnumVO.HTTP.getCode())
                .parameterJson("{}")
                .build());
        notifyTasks.add(NotifyTaskEntity.builder()
                .teamId("TEAM100")
                .notifyUrl("http://example.com/notify")
                .notifyCount(0)
                .notifyType(NotifyTypeEnumVO.HTTP.getCode())
                .parameterJson("{}")
                .build());

        when(repository.queryUnExecutedNotifyTaskList()).thenReturn(notifyTasks);
        when(port.groupBuyNotify(any())).thenReturn(NotifyTaskHTTPEnumVO.SUCCESS.getCode());
        when(repository.updateNotifyTaskStatusSuccess(anyString())).thenReturn(1);

        java.util.Map<String, Integer> result = settlementService.execSettlementNotifyJob();

        // 验证两个任务都成功处理，与顺序无关
        assertEquals(2, result.get("successCount").intValue());
    }
}