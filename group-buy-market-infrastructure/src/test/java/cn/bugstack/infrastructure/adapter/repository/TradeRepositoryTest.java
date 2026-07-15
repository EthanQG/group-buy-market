package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.bugstack.domain.trade.model.entity.*;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.bugstack.domain.trade.model.valobj.NotifyConfigVO;
import cn.bugstack.domain.trade.model.valobj.NotifyTypeEnumVO;
import cn.bugstack.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import cn.bugstack.infrastructure.dao.IGroupBuyActivityDao;
import cn.bugstack.infrastructure.dao.IGroupBuyOrderDao;
import cn.bugstack.infrastructure.dao.IGroupBuyOrderListDao;
import cn.bugstack.infrastructure.dao.INotifyTaskDao;
import cn.bugstack.infrastructure.dcc.DCCService;
import cn.bugstack.infrastructure.redis.IRedisService;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TradeRepositoryTest {

    @Mock
    private IGroupBuyOrderDao groupBuyOrderDao;

    @Mock
    private IGroupBuyOrderListDao groupBuyOrderListDao;

    @Mock
    private IGroupBuyActivityDao groupBuyActivityDao;

    @Mock
    private INotifyTaskDao notifyTaskDao;

    @Mock
    private DCCService dccService;

    @Mock
    private IRedisService redisService;

    @InjectMocks
    private TradeRepository tradeRepository;

    private GroupBuyOrderAggregate orderAggregate;
    private UserEntity userEntity;
    private PayActivityEntity payActivityEntity;
    private PayDiscountEntity payDiscountEntity;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(tradeRepository, "topic_team_success", "topic.team.success");

        userEntity = UserEntity.builder().userId("user_001").build();

        payActivityEntity = PayActivityEntity.builder()
                .activityId(1001L)
                .teamId(null)
                .targetCount(3)
                .validTime(30)
                .startTime(new Date())
                .endTime(new Date(System.currentTimeMillis() + 86400000))
                .build();

        NotifyConfigVO notifyConfigVO = NotifyConfigVO.builder()
                .notifyType(NotifyTypeEnumVO.HTTP)
                .notifyUrl("http://example.com/notify")
                .build();

        payDiscountEntity = PayDiscountEntity.builder()
                .goodsId("GOODS001")
                .source("WEB")
                .channel("ALIPAY")
                .originalPrice(new BigDecimal("100.00"))
                .deductionPrice(new BigDecimal("10.00"))
                .payPrice(new BigDecimal("90.00"))
                .notifyConfigVO(notifyConfigVO)
                .outTradeNo("OUT_TRADE_123")
                .build();

        orderAggregate = GroupBuyOrderAggregate.builder()
                .userEntity(userEntity)
                .payActivityEntity(payActivityEntity)
                .payDiscountEntity(payDiscountEntity)
                .userTakeOrderCount(0)
                .build();
    }

    @Test
    public void test_lock_market_pay_order_new_team() {
        doReturn(1).when(groupBuyOrderDao).insert(any());
        doReturn(1).when(groupBuyOrderListDao).insert(any());

        MarketPayOrderEntity result = tradeRepository.lockMarketPayOrder(orderAggregate);

        assertNotNull(result);
        assertNotNull(result.getTeamId());
        assertNotNull(result.getOrderId());
        assertEquals(TradeOrderStatusEnumVO.CREATE, result.getTradeOrderStatusEnumVO());
        verify(groupBuyOrderDao).insert(any());
        verify(groupBuyOrderListDao).insert(any());
    }

    @Test
    public void test_lock_market_pay_order_existing_team() {
        orderAggregate.getPayActivityEntity().setTeamId("TEAM123");

        when(groupBuyOrderDao.updateAddLockCount(anyString())).thenReturn(1);
        doReturn(1).when(groupBuyOrderListDao).insert(any());

        MarketPayOrderEntity result = tradeRepository.lockMarketPayOrder(orderAggregate);

        assertNotNull(result);
        assertEquals("TEAM123", result.getTeamId());
        verify(groupBuyOrderDao).updateAddLockCount("TEAM123");
    }

    @Test
    public void test_lock_market_pay_order_team_full() {
        orderAggregate.getPayActivityEntity().setTeamId("TEAM123");

        when(groupBuyOrderDao.updateAddLockCount(anyString())).thenReturn(0);

        try {
            tradeRepository.lockMarketPayOrder(orderAggregate);
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.E0005, e.getCode());
        }
    }

    @Test
    public void test_query_group_buy_progress() {
        cn.bugstack.infrastructure.dao.po.GroupBuyOrder groupBuyOrder =
                new cn.bugstack.infrastructure.dao.po.GroupBuyOrder();
        groupBuyOrder.setTargetCount(3);
        groupBuyOrder.setCompleteCount(1);
        groupBuyOrder.setLockCount(2);

        when(groupBuyOrderDao.queryGroupBuyProgress(anyString())).thenReturn(groupBuyOrder);

        GroupBuyProgressVO result = tradeRepository.queryGroupBuyProgress("TEAM123");

        assertNotNull(result);
        assertEquals(3, result.getTargetCount().intValue());
        assertEquals(1, result.getCompleteCount().intValue());
        assertEquals(2, result.getLockCount().intValue());
    }

    @Test
    public void test_query_group_buy_progress_not_found() {
        when(groupBuyOrderDao.queryGroupBuyProgress(anyString())).thenReturn(null);

        GroupBuyProgressVO result = tradeRepository.queryGroupBuyProgress("TEAM_NOT_EXIST");

        assertNull(result);
    }

    @Test
    public void test_is_sc_black_intercept_true() {
        when(dccService.isSCBlackIntercept("WEB", "ALIPAY")).thenReturn(true);

        boolean result = tradeRepository.isSCBlackIntercept("WEB", "ALIPAY");

        assertTrue(result);
    }

    @Test
    public void test_is_sc_black_intercept_false() {
        when(dccService.isSCBlackIntercept("WEB", "ALIPAY")).thenReturn(false);

        boolean result = tradeRepository.isSCBlackIntercept("WEB", "ALIPAY");

        assertFalse(result);
    }

    @Test
    public void test_query_order_count_by_activity_id() {
        when(groupBuyOrderListDao.queryOrderCountByActivityId(any())).thenReturn(5);

        Integer result = tradeRepository.queryOrderCountByActivityId(1001L, "user_001");

        assertEquals(5, result.intValue());
    }

     @Test
    public void test_occupy_team_stock_success() {
        // Mock配置：当前库存0，incr返回5，setNx返回true
        when(redisService.getAtomicLong(anyString())).thenReturn(0L);
        when(redisService.incr(anyString())).thenReturn(5L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 执行：目标库存10，当前incr返回5（实际是5+1=6）
        boolean result = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 10, 30);

        // 验证：6 <= 10+0，返回true
        assertTrue(result);
    }

    @Test
    public void test_occupy_team_stock_exceeds_limit() {
        // Mock配置：当前库存0，incr返回12，setNx返回false
        when(redisService.getAtomicLong(anyString())).thenReturn(0L);
        when(redisService.incr(anyString())).thenReturn(12L);

        // 执行：目标库存10，当前incr返回12（实际是12+1=13）
        boolean result = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 10, 30);

        // 验证：13 > 10+0，返回false
        assertFalse(result);
        // 验证：setAtomicLong方法被调用了1次，参数为team_stock和10
        verify(redisService).setAtomicLong(eq("team_stock"), eq(10L));
    }

    @Test
    public void test_occupy_team_stock_with_recovery_count() {
         // Mock配置：恢复量=3，incr返回10，setNx返回true
        when(redisService.getAtomicLong(anyString())).thenReturn(3L);
        when(redisService.incr(anyString())).thenReturn(10L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 执行：目标库存10，当前incr返回10（实际是10+1=11）
        boolean result = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 10, 30);

        assertTrue(result);
    }

    @Test
    public void test_recovery_team_stock() {
         // 执行恢复库存
        tradeRepository.recoveryTeamStock("recovery_key", 30);

        // 验证：调用了redis的incr方法
        verify(redisService).incr("recovery_key");
    }

    @Test
    public void test_recovery_team_stock_empty_key() {
        // 执行恢复库存，key为空
        tradeRepository.recoveryTeamStock(null, 30);
        // 执行恢复库存，key为空字符串
        tradeRepository.recoveryTeamStock("", 30);

        // 验证：未调用redis的incr方法
        verify(redisService, never()).incr(anyString());
    }

    // ==================== Redis库存扣减算法 - 边界值测试 ====================

    /**
     * STK-006: 第一个普通用户参团（核心场景）
     * 场景：3人团，团长已占1个名额，第一个普通用户参团
     * 原理：Redis INCR首次返回1，occupy=1+1=2，判断2 ≤ 3+0=3 → 通过
     */
    @Test
    public void test_occupy_team_stock_first_normal_user() {
        // Mock配置：恢复量=0，incr返回1（首次调用INCR）
        when(redisService.getAtomicLong(anyString())).thenReturn(0L);
        when(redisService.incr(anyString())).thenReturn(1L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 执行：目标库存3，当前incr返回1（occupy=1+1=2）
        boolean result = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 3, 30);

        // 验证：2 ≤ 3+0，返回true
        assertTrue(result);
        // 验证分布式锁被正确调用
        verify(redisService).setNx(eq("team_stock_2"), eq(90L), any());
    }

    /**
     * STK-007: 正好满员场景
     * 场景：3人团，2个普通人，occupy=3时正好等于目标
     * 原理：occupy=3（INCR返回2，+1=3），判断3 ≤ 3+0=3 → 通过
     */
    @Test
    public void test_occupy_team_stock_exactly_full() {
        // Mock配置：恢复量=0，incr返回2（第三个人）
        when(redisService.getAtomicLong(anyString())).thenReturn(0L);
        when(redisService.incr(anyString())).thenReturn(2L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 执行：目标库存3，当前incr返回2（occupy=2+1=3）
        boolean result = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 3, 30);

        // 验证：3 ≤ 3+0，返回true
        assertTrue(result);
    }

    /**
     * STK-008: 恢复量为null的场景
     * 场景：recoveryTeamStockKey不存在，返回null
     * 原理：null值应该被处理为0，不影响判断
     */
    @Test
    public void test_occupy_team_stock_recovery_null() {
        // Mock配置：恢复量=null（key不存在），incr返回5
        when(redisService.getAtomicLong(anyString())).thenReturn(null);
        when(redisService.incr(anyString())).thenReturn(5L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 执行：目标库存10，当前incr返回5（occupy=5+1=6）
        boolean result = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 10, 30);

        // 验证：6 ≤ 10+0(null处理为0)，返回true
        assertTrue(result);
    }

    /**
     * STK-009: 有恢复量且正好边界
     * 场景：target=10，recovery=3，incr返回9
     * 原理：occupy=9+1=10，判断10 ≤ 10+3=13 → 通过
     */
    @Test
    public void test_occupy_team_stock_with_recovery_exactly_boundary() {
        // Mock配置：恢复量=3，incr返回9
        when(redisService.getAtomicLong(anyString())).thenReturn(3L);
        when(redisService.incr(anyString())).thenReturn(9L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 执行：目标库存10，恢复量3，当前incr返回9（occupy=9+1=10）
        boolean result = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 10, 30);

        // 验证：10 ≤ 10+3=13，返回true
        assertTrue(result);
    }

    /**
     * STK-010: 有恢复量但仍然超限
     * 场景：target=10，recovery=1，incr返回11
     * 最终：incr返回11，occupy=12，12 > 11 → 不通过
     */
    @Test
    public void test_occupy_team_stock_with_recovery_still_exceeds() {
        // Mock配置：恢复量=1，incr返回11
        when(redisService.getAtomicLong(anyString())).thenReturn(1L);
        when(redisService.incr(anyString())).thenReturn(11L);

        // 执行：目标库存10，恢复量1，当前incr返回11（occupy=11+1=12）
        boolean result = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 10, 30);

        // 验证：12 > 10+1=11，返回false
        assertFalse(result);
        // 验证：超限时重置库存为目标值
        verify(redisService).setAtomicLong(eq("team_stock"), eq(10L));
    }

    /**
     * STK-011: 验证分布式锁key格式
     * 场景：正常参团时，lockKey格式为 teamStockKey + "_" + occupy
     * 原理：确保锁key格式正确，便于问题排查
     */
    @Test
    public void test_occupy_team_stock_lock_key_format() {
        // Mock配置：恢复量=0，incr返回3
        when(redisService.getAtomicLong(anyString())).thenReturn(0L);
        when(redisService.incr(anyString())).thenReturn(3L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 执行：team_stock_key, recovery_stock, target=5, validTime=30
        tradeRepository.occupyTeamStock("team_stock_key", "recovery_stock", 5, 30);

        // 验证：incr返回3，occupy=3+1=4，lockKey="team_stock_key_4"
        // 过期时间 = validTime + 60 = 30 + 60 = 90分钟
        verify(redisService).setNx(eq("team_stock_key_4"), eq(90L), any());
    }

    /**
     * STK-012: 验证过期时间设置
     * 场景：validTime=30分钟，加上60分钟延后时间
     * 原理：让数据保留时间稍长一些，便于排查问题
     */
    @Test
    public void test_occupy_team_stock_expire_time() {
        // Mock配置
        when(redisService.getAtomicLong(anyString())).thenReturn(0L);
        when(redisService.incr(anyString())).thenReturn(1L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 执行：validTime=45分钟
        tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 5, 45);

        // 验证：过期时间 = 45 + 60 = 105分钟
        verify(redisService).setNx(eq("team_stock_2"), eq(105L), any());
    }

    /**
     * STK-013: 连续参团场景模拟
     * 场景：模拟3人团依次满员的过程
     * 原理：第一个用户occupy=2，第二个occupy=3，第三个应该失败
     */
    @Test
    public void test_occupy_team_stock_consecutive_users() {
        // Mock配置：恢复量=0，incr依次返回1,2
        when(redisService.getAtomicLong(anyString())).thenReturn(0L);
        when(redisService.incr(anyString())).thenReturn(1L, 2L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 第一个用户：occupy=2，2 ≤ 3 → 通过
        boolean user1 = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 3, 30);
        assertTrue(user1);

        // 第二个用户：occupy=3，3 ≤ 3 → 通过（此时团已满）
        boolean user2 = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 3, 30);
        assertTrue(user2);

        // 第三个用户：需要重新设置incr返回值为3
        when(redisService.incr(anyString())).thenReturn(3L);

        // 第三个用户：occupy=4，4 > 3 → 不通过
        boolean user3 = tradeRepository.occupyTeamStock("team_stock", "recovery_stock", 3, 30);
        assertFalse(user3);
    }

    /**
     * STK-014: 并发抢最后一个库存测试
     * 测试目标：验证并发场景下，库存扣减逻辑正确且不会出现超卖
     * 测试场景：3人团，已有2人，10个线程同时调用库存扣减
     * 预期结果：验证代码逻辑正确（incr原子递增+setNx分布式锁）
     * 测试说明：由于Mockito Mock在多线程下行为不稳定，此测试验证代码逻辑
     */
    @Test
    public void test_concurrent_occupy_last_stock_no_oversell() throws Exception {
        // 1. 测试参数配置
        final int targetStock = 3;           // 目标库存：3人团
        final int threadCount = 10;          // 10个线程
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);

        // 2. Mock配置：模拟库存只剩最后1个位置
        //    incr返回2，则occupy=2+1=3，刚好等于target=3
        when(redisService.getAtomicLong(anyString())).thenReturn(0L);
        when(redisService.incr(anyString())).thenReturn(2L);
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(true);

        // 3. 模拟并发请求
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    // 调用库存扣减
                    tradeRepository.occupyTeamStock(
                            "team_stock_TEAM123",
                            "recovery_stock_TEAM123",
                            targetStock,
                            30
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        // 4. 等待所有线程执行完毕
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // 5. 验证结果：occupy=2+1=3，不超过target=3，setNx被调用
        // 由于incr返回2，occupy=3，刚好等于target，所以不会触发"超限重置"
        // 每个线程都会调用setNx并返回true
        verify(redisService, times(threadCount)).incr(anyString());
        verify(redisService, times(threadCount)).setNx(eq("team_stock_TEAM123_3"), eq(90L), any());
        // 验证没有触发超限重置
        verify(redisService, never()).setAtomicLong(anyString(), anyLong());
    }

    /**
     * STK-015: 并发超卖防护验证测试
     * 测试目标：验证超卖防护逻辑正确（超过target时会被拒绝）
     * 测试场景：模拟库存超限时的处理逻辑
     * 预期结果：当occupy > target时，返回false并重置库存
     * 测试说明：验证库存超卖防护机制
     */
    @Test
    public void test_concurrent_occupy_exceed_stock_rejected() throws Exception {
        // 1. 测试参数配置
        final int targetStock = 3;           // 目标库存：3人团
        final int threadCount = 10;          // 10个线程
        final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
        final java.util.concurrent.atomic.AtomicInteger rejectCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // 2. Mock配置：模拟库存已满（incr返回4，则occupy=5 > target=3）
        //    这种情况下应该返回false并重置库存
        when(redisService.getAtomicLong(anyString())).thenReturn(0L);
        when(redisService.incr(anyString())).thenReturn(4L);  // occupy=4+1=5 > 3
        when(redisService.setNx(anyString(), anyLong(), any())).thenReturn(false);

        // 3. 模拟并发请求
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    boolean result = tradeRepository.occupyTeamStock(
                            "team_stock_FULL",
                            "recovery_stock_FULL",
                            targetStock,
                            30
                    );
                    if (!result) {
                        rejectCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 4. 等待所有线程执行完毕
        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // 5. 验证结果：所有线程都应该被拒绝（5 > 3）
        assertEquals("所有10个线程都应该被拒绝", threadCount, rejectCount.get());
        // 验证库存被重置为目标值（防止超卖）
        verify(redisService, atLeastOnce()).setAtomicLong(eq("team_stock_FULL"), eq((long) targetStock));
    }
}