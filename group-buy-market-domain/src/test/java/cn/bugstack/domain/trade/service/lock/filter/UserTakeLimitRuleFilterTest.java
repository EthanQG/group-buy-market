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
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserTakeLimitRuleFilterTest {

    @Mock
    private ITradeRepository repository;

    @org.mockito.Spy
    @InjectMocks
    private UserTakeLimitRuleFilter filter;

    private TradeLockRuleCommandEntity commandEntity;
    private TradeLockRuleFilterFactory.DynamicContext dynamicContext;

    @Before
    public void setUp() {
        commandEntity = TradeLockRuleCommandEntity.builder()
                .activityId(1001L)
                .userId("user_001")
                .teamId(null)
                .build();
        dynamicContext = new TradeLockRuleFilterFactory.DynamicContext();
        // 模拟活动参与次数限制为5次
        GroupBuyActivityEntity activity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .activityName("测试活动")
                .status(ActivityStatusEnumVO.EFFECTIVE)
                .takeLimitCount(5)
                .startTime(new Date(System.currentTimeMillis() - 86400000))
                .endTime(new Date(System.currentTimeMillis() + 86400000))
                .build();
        dynamicContext.setGroupBuyActivity(activity);
    }

    private void mockNextFilter() throws Exception {
        // 模拟下一个过滤器返回空，设置预设和行为
        doReturn(TradeLockRuleFilterBackEntity.builder().build())
        .when(filter).next(any(), any());
    }

    @Test
    public void test_user_within_limit() throws Exception {
        // 模拟用户在该活动下的参与次数为2次
        when(repository.queryOrderCountByActivityId(anyLong(), anyString())).thenReturn(2);

        mockNextFilter();

        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        assertNotNull(result);
        assertEquals(2, dynamicContext.getUserTakeOrderCount().intValue());
    }

    @Test
    public void test_user_at_limit_boundary() throws Exception {
        // 模拟用户在该活动下的参与次数为5次
        when(repository.queryOrderCountByActivityId(anyLong(), anyString())).thenReturn(5);
        mockNextFilter();
        
        try {
            filter.apply(commandEntity, dynamicContext);
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals("E0103", e.getCode());
        } catch (Exception e) {
            fail("Expected AppException");
        }
    }

    @Test
    public void test_user_exceeds_limit() throws Exception {
        when(repository.queryOrderCountByActivityId(anyLong(), anyString())).thenReturn(6);
        mockNextFilter();
        try {
            filter.apply(commandEntity, dynamicContext);
            fail("Expected AppException");
        } catch (AppException e) {
             assertEquals("E0103", e.getCode());
        } catch (Exception e) {
            fail("Expected AppException");
        }
    }

    @Test
    public void test_user_firstParticipation() throws Exception {
        when(repository.queryOrderCountByActivityId(anyLong(), anyString())).thenReturn(0);
        mockNextFilter();

        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        assertNotNull(result);
        assertEquals(0, dynamicContext.getUserTakeOrderCount().intValue());
    }

    @Test
    public void test_user_one_less_than_limit() throws Exception {
        when(repository.queryOrderCountByActivityId(anyLong(), anyString())).thenReturn(4);
        mockNextFilter();

        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        assertNotNull(result);
        assertEquals(4, dynamicContext.getUserTakeOrderCount().intValue());
    }

    @Test
    public void test_unlimited_activity_null_takeLimitCount() throws Exception {
        GroupBuyActivityEntity unlimitedActivity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .activityName("无限参与活动")
                .status(ActivityStatusEnumVO.EFFECTIVE)
                .takeLimitCount(null)
                .startTime(new Date(System.currentTimeMillis() - 86400000))
                .endTime(new Date(System.currentTimeMillis() + 86400000))
                .build();
        dynamicContext.setGroupBuyActivity(unlimitedActivity);

        when(repository.queryOrderCountByActivityId(anyLong(), anyString())).thenReturn(100);
        mockNextFilter();
        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        assertNotNull(result);
        assertEquals(100, dynamicContext.getUserTakeOrderCount().intValue());
    }

    @Test
    public void test_unlimited_activity_zero_takeLimitCount() throws Exception {
        // 模拟活动参与次数限制为0次,即不允许用户参与拼团
        GroupBuyActivityEntity unlimitedActivity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .activityName("不允许用户参与拼团")
                .status(ActivityStatusEnumVO.EFFECTIVE)
                .takeLimitCount(0)
                .startTime(new Date(System.currentTimeMillis() - 86400000))
                .endTime(new Date(System.currentTimeMillis() + 86400000))
                .build();
        dynamicContext.setGroupBuyActivity(unlimitedActivity);

        when(repository.queryOrderCountByActivityId(anyLong(), anyString())).thenReturn(0);
        mockNextFilter();
        
        try {
            filter.apply(commandEntity, dynamicContext);
            fail("Expected AppException");
        } catch (AppException e) {
             assertEquals("E0103", e.getCode());
        } catch (Exception e) {
            fail("Expected AppException");
        }
    }
}