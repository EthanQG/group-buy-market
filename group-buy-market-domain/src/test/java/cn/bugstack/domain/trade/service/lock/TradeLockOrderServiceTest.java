package cn.bugstack.domain.trade.service.lock;

import cn.bugstack.domain.trade.adapter.repository.ITradeRepository;
import cn.bugstack.domain.trade.model.entity.*;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;
import cn.bugstack.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import cn.bugstack.types.design.framework.link.model2.chain.BusinessLinkedList;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TradeLockOrderServiceTest {

    @Mock
    private ITradeRepository repository;

    @Mock
    private BusinessLinkedList<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> tradeRuleFilter;

    @InjectMocks
    private TradeLockOrderService tradeLockOrderService;

    private UserEntity userEntity;
    private PayActivityEntity payActivityEntity;
    private PayDiscountEntity payDiscountEntity;

    @Before
    public void setUp() {
        userEntity = UserEntity.builder()
                .userId("user_001")
                .build();

        payActivityEntity = PayActivityEntity.builder()
                .activityId(1001L)
                .teamId(null)
                .targetCount(3)
                .validTime(30)
                .startTime(new Date())
                .endTime(new Date(System.currentTimeMillis() + 86400000))
                .build();

        payDiscountEntity = PayDiscountEntity.builder()
                .goodsId("GOODS001")
                .source("WEB")
                .channel("ALIPAY")
                .originalPrice(new BigDecimal("100.00"))
                .deductionPrice(new BigDecimal("10.00"))
                .payPrice(new BigDecimal("90.00"))
                .outTradeNo("OUT_TRADE_123")
                .build();
    }

    // 测试锁定新团队订单成功 用户发起一个新的拼团（团长）
    @Test
    public void test_lock_order_success_new_team() throws Exception {
        TradeLockRuleFilterBackEntity filterBack = TradeLockRuleFilterBackEntity.builder()       
        .userTakeOrderCount(0)//团长没有参加过任何拼团订单 
                .build();

        //Mock过滤器返回（团长不占库存，不需要recoveryKey）
        when(tradeRuleFilter.apply(any(), any())).thenReturn(filterBack);

        MarketPayOrderEntity mockOrder = MarketPayOrderEntity.builder()
                .teamId("TEAM_NEW")
                .orderId("ORDER123")
                .originalPrice(new BigDecimal("100.00"))
                .deductionPrice(new BigDecimal("10.00"))
                .payPrice(new BigDecimal("90.00"))
                .tradeOrderStatusEnumVO(cn.bugstack.domain.trade.model.valobj.TradeOrderStatusEnumVO.CREATE)
                .build();

        when(repository.lockMarketPayOrder(any())).thenReturn(mockOrder);

        MarketPayOrderEntity result = tradeLockOrderService.lockMarketPayOrder(userEntity, payActivityEntity, payDiscountEntity);

        assertNotNull(result);
        assertEquals("TEAM_NEW", result.getTeamId());
        assertEquals("ORDER123", result.getOrderId());
    }

    // 测试锁定已团队订单成功 普通用户加入已有的拼团（非团长）
    @Test
    public void test_lock_order_success_existing_team() throws Exception {
        payActivityEntity = PayActivityEntity.builder()
                .activityId(1001L)
                .teamId("TEAM123")//已有的拼团团队
                .targetCount(3)
                .validTime(30)
                .startTime(new Date())
                .endTime(new Date(System.currentTimeMillis() + 86400000))
                .build();

        //Mock过滤器返回（老团需要校验库存）
        TradeLockRuleFilterBackEntity filterBack = TradeLockRuleFilterBackEntity.builder()
                .userTakeOrderCount(1)//普通用户已加入过拼团
                .recoveryTeamStockKey("recovery_key")//恢复库存的key
                .build();

        when(tradeRuleFilter.apply(any(), any())).thenReturn(filterBack);

        MarketPayOrderEntity mockOrder = MarketPayOrderEntity.builder()
                .teamId("TEAM123")
                .orderId("ORDER456")
                .originalPrice(new BigDecimal("100.00"))
                .deductionPrice(new BigDecimal("10.00"))
                .payPrice(new BigDecimal("90.00"))
                .tradeOrderStatusEnumVO(cn.bugstack.domain.trade.model.valobj.TradeOrderStatusEnumVO.CREATE)
                .build();

        when(repository.lockMarketPayOrder(any())).thenReturn(mockOrder);

        MarketPayOrderEntity result = tradeLockOrderService.lockMarketPayOrder(userEntity, payActivityEntity, payDiscountEntity);

        assertNotNull(result);
        assertEquals("TEAM123", result.getTeamId());
    }

    //锁单失败触发库存恢复 (锁单成功但后续步骤失败，需要恢复库存)
    @Test
    public void test_lock_order_failure_with_stock_recovery() throws Exception {
        TradeLockRuleFilterBackEntity filterBack = TradeLockRuleFilterBackEntity.builder()
                .userTakeOrderCount(0)
                .recoveryTeamStockKey("recovery_key")//恢复库存的key
                .build();

        when(tradeRuleFilter.apply(any(), any())).thenReturn(filterBack);
        //Mock锁单抛出异常（模拟数据库插入失败）
        when(repository.lockMarketPayOrder(any())).thenThrow(new AppException(ResponseCode.INDEX_EXCEPTION));
        //Mock恢复库存方法（防止NPE）
        doNothing().when(repository).recoveryTeamStock(anyString(), anyInt());

        try {
            tradeLockOrderService.lockMarketPayOrder(userEntity, payActivityEntity, payDiscountEntity);
            fail("Expected AppException");
        } catch (AppException e) {
            verify(repository).recoveryTeamStock(eq("recovery_key"), eq(30));
        }
    }

    // 测试锁定新团队订单失败 拼团活动不可用
    @Test
    public void test_lock_order_activity_unavailable() throws Exception {
        doThrow(new AppException(ResponseCode.E0101)).when(tradeRuleFilter).apply(any(), any());

        try {
            tradeLockOrderService.lockMarketPayOrder(userEntity, payActivityEntity, payDiscountEntity);
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.E0101.getCode(), e.getCode());
        }
    }

    //用户超限
    @Test
    public void test_lock_order_user_limit_exceeded() throws Exception {
        doThrow(new AppException(ResponseCode.E0103)).when(tradeRuleFilter).apply(any(), any());

        try {
            tradeLockOrderService.lockMarketPayOrder(userEntity, payActivityEntity, payDiscountEntity);
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.E0103.getCode(), e.getCode());
        }
    }

    //库存已满
    @Test
    public void test_lock_order_team_stock_full() throws Exception {
        TradeLockRuleFilterBackEntity filterBack = TradeLockRuleFilterBackEntity.builder()
                .userTakeOrderCount(0)
                .recoveryTeamStockKey("recovery_key")
                .build();

        // 方法签名声明了 throws Exception，所以不需要 try-catch
        when(tradeRuleFilter.apply(any(), any())).thenReturn(filterBack);
        
        //Mock锁单抛出异常（模拟缓存库存不足）
        when(repository.lockMarketPayOrder(any())).thenThrow(new AppException(ResponseCode.E0008));
        //Mock恢复库存
        doNothing().when(repository).recoveryTeamStock(anyString(), anyInt());

        try {
            tradeLockOrderService.lockMarketPayOrder(userEntity, payActivityEntity, payDiscountEntity);
            fail("Expected Exception");
        } catch (Exception e) {
            // 验证库存恢复方法被调用
            assertTrue(e instanceof AppException);
            assertEquals(ResponseCode.E0008.getCode(), ((AppException) e).getCode());
            verify(repository).recoveryTeamStock(eq("recovery_key"), eq(30));
        }
    }

    @Test
    public void test_query_no_pay_order() {
        MarketPayOrderEntity mockOrder = MarketPayOrderEntity.builder()
                .teamId("TEAM123")
                .orderId("ORDER123")
                .tradeOrderStatusEnumVO(cn.bugstack.domain.trade.model.valobj.TradeOrderStatusEnumVO.CREATE)
                .build();

        when(repository.queryMarketPayOrderEntityByOutTradeNo(anyString(), anyString())).thenReturn(mockOrder);

        MarketPayOrderEntity result = tradeLockOrderService.queryNoPayMarketPayOrderByOutTradeNo("user_001", "OUT_TRADE_123");

        assertNotNull(result);
        assertEquals("TEAM123", result.getTeamId());
    }

    @Test
    public void test_query_group_buy_progress() {
        GroupBuyProgressVO mockProgress = GroupBuyProgressVO.builder()
                .targetCount(3)
                .completeCount(1)
                .lockCount(2)
                .build();

        when(repository.queryGroupBuyProgress(anyString())).thenReturn(mockProgress);

        GroupBuyProgressVO result = tradeLockOrderService.queryGroupBuyProgress("TEAM123");

        assertNotNull(result);
        assertEquals(3, result.getTargetCount().intValue());
        assertEquals(1, result.getCompleteCount().intValue());
        assertEquals(2, result.getLockCount().intValue());
    }

    @Test
    public void test_duplicate_order_lock() {
        TradeLockRuleFilterBackEntity filterBack = TradeLockRuleFilterBackEntity.builder()
                .userTakeOrderCount(0)
                .build();

        try {
            when(tradeRuleFilter.apply(any(), any())).thenReturn(filterBack);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //INDEX_EXCEPTION 是数据库唯一索引冲突抛出的异常，表示重复订单。
        when(repository.lockMarketPayOrder(any())).thenThrow(new AppException(ResponseCode.INDEX_EXCEPTION));

        try {
            tradeLockOrderService.lockMarketPayOrder(userEntity, payActivityEntity, payDiscountEntity);
            fail("Expected Exception");
        } catch (Exception e) {
            assertTrue(e instanceof AppException);
            assertEquals(ResponseCode.INDEX_EXCEPTION.getCode(), ((AppException) e).getCode());
        }
    }
}