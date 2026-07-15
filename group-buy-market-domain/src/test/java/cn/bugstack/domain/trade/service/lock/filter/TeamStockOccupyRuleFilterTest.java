package cn.bugstack.domain.trade.service.lock.filter;

import cn.bugstack.domain.trade.adapter.repository.ITradeRepository;
import cn.bugstack.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.bugstack.domain.trade.model.entity.TradeLockRuleCommandEntity;
import cn.bugstack.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import cn.bugstack.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import cn.bugstack.types.enums.ActivityStatusEnumVO;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TeamStockOccupyRuleFilterTest {

    @Mock
    private ITradeRepository repository;

    @Spy
    @InjectMocks
    private TeamStockOccupyRuleFilter filter;

    private TradeLockRuleFilterFactory.DynamicContext dynamicContext;

    @Before
    public void setUp() {
        dynamicContext = new TradeLockRuleFilterFactory.DynamicContext();
        GroupBuyActivityEntity activity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .activityName("测试活动")
                .status(ActivityStatusEnumVO.EFFECTIVE)
                .target(10)
                .validTime(30)
                .startTime(new Date(System.currentTimeMillis() - 86400000))
                .endTime(new Date(System.currentTimeMillis() + 86400000))
                .build();
        dynamicContext.setGroupBuyActivity(activity);
        dynamicContext.setUserTakeOrderCount(0);  // 设置初始值，避免 NPE
    }

    private void mockNextFilter() throws Exception {
        doReturn(TradeLockRuleFilterBackEntity.builder().build())
                .when(filter).next(any(), any());
    }

    @Test
    public void test_new_team_no_stock_check() throws Exception {
        TradeLockRuleCommandEntity commandEntity = TradeLockRuleCommandEntity.builder()
                .activityId(1001L)
                .userId("user_001")
                .teamId(null)
                .build();

        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        System.out.println("========== 详细字段信息 ==========");
        System.out.println("result 是否为 null: " + (result == null));
        System.out.println("userTakeOrderCount: " + result.getUserTakeOrderCount());
        System.out.println("recoveryTeamStockKey: " + result.getRecoveryTeamStockKey());
        // 如果还有其他字段，也可以打印
        System.out.println("==================================");
        assertNotNull(result);
        assertNull(result.getRecoveryTeamStockKey());
        assertEquals(0, dynamicContext.getUserTakeOrderCount().intValue());
    }

    @Test
    public void test_existing_team_stock_occupy_success() throws Exception {
        TradeLockRuleCommandEntity commandEntity = TradeLockRuleCommandEntity.builder()
                .activityId(1001L)
                .userId("user_001")
                .teamId("TEAM123")
                .build();

        when(repository.occupyTeamStock(anyString(), anyString(), anyInt(), anyInt())).thenReturn(true);
        mockNextFilter();

        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        System.out.println("========== 详细字段信息 ==========");
        System.out.println("result 是否为 null: " + (result == null));
        System.out.println("userTakeOrderCount: " + result.getUserTakeOrderCount());
        System.out.println("recoveryTeamStockKey: " + result.getRecoveryTeamStockKey());
        // 如果还有其他字段，也可以打印
        System.out.println("==================================");
        assertNotNull(result);
        assertNotNull(result.getRecoveryTeamStockKey());
    }

    @Test
    public void test_existing_team_stock_occupy_failed() throws Exception {
        TradeLockRuleCommandEntity commandEntity = TradeLockRuleCommandEntity.builder()
                .activityId(1001L)
                .userId("user_001")
                .teamId("TEAM123")
                .build();

        when(repository.occupyTeamStock(anyString(), anyString(), anyInt(), anyInt())).thenReturn(false);
        mockNextFilter();
        try {
            filter.apply(commandEntity, dynamicContext);
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.E0008.getCode(), e.getCode());
        }
    }

    @Test
    public void test_stock_with_recovery_count() throws Exception {
        TradeLockRuleCommandEntity commandEntity = TradeLockRuleCommandEntity.builder()
                .activityId(1001L)
                .userId("user_001")
                .teamId("TEAM123")
                .build();

        when(repository.occupyTeamStock(anyString(), anyString(), anyInt(), anyInt())).thenReturn(true);
        mockNextFilter();

        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        assertNotNull(result);
        assertNotNull(result.getRecoveryTeamStockKey());
    }

    @Test
    public void test_concurrent_occupy_last_slot() throws Exception {
        TradeLockRuleCommandEntity commandEntity = TradeLockRuleCommandEntity.builder()
                .activityId(1001L)
                .userId("user_concurrent")
                .teamId("TEAM123")
                .build();

        when(repository.occupyTeamStock(anyString(), anyString(), anyInt(), anyInt())).thenReturn(false);
        mockNextFilter();
        
        try {
            filter.apply(commandEntity, dynamicContext);
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.E0008.getCode(), e.getCode());
        }
    }
}