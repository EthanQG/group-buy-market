package cn.bugstack.domain.trade.service.lock.filter;

import cn.bugstack.domain.trade.adapter.repository.ITradeRepository;
import cn.bugstack.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.bugstack.domain.trade.model.entity.TradeLockRuleCommandEntity;
import cn.bugstack.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import cn.bugstack.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import cn.bugstack.types.enums.ActivityStatusEnumVO;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ActivityUsabilityRuleFilterTest {

    // 1. 模拟数据库查询活动信息
    @Mock
    private ITradeRepository repository;

    // 2. 注入测试类
    @InjectMocks
    @org.mockito.Spy
    private ActivityUsabilityRuleFilter filter;

    private TradeLockRuleCommandEntity commandEntity;
    private TradeLockRuleFilterFactory.DynamicContext dynamicContext;

    @Before
    public void setUp() {
        // 3. 初始化测试数据
        commandEntity = TradeLockRuleCommandEntity.builder()
                .activityId(1001L)
                .userId("user_001")
                .teamId(null)
                .build();
        dynamicContext = new TradeLockRuleFilterFactory.DynamicContext();
    }

    private void mockNextFilter() throws Exception {
        // 模拟下一个过滤器返回空，设置预设和行为
        doReturn(TradeLockRuleFilterBackEntity.builder().build())
        .when(filter).next(any(), any());
    }

    @Test
    public void test_activity_effective_and_in_valid_period() throws Exception {
        //1. 模拟活动信息
        GroupBuyActivityEntity activity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .activityName("测试活动")
                .status(ActivityStatusEnumVO.EFFECTIVE)
                // 模拟活动开始时间在1天前，结束时间在1天后
                .startTime(new Date(System.currentTimeMillis() - 86400000))
                .endTime(new Date(System.currentTimeMillis() + 86400000))
                .build();

        // 2. 模拟数据库查询活动信息
        when(repository.queryGroupBuyActivityEntityByActivityId(anyLong())).thenReturn(activity);
        // 模拟下一个过滤器返回空
        mockNextFilter();
        //3.执行测试
        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        // 4. 断言结果
        assertNotNull(result);
        // 断言活动实体对象是否与上下文中的活动实体对象相同
        assertEquals(activity, dynamicContext.getGroupBuyActivity());
    }

    @Test
    public void test_activity_not_effective() {
        GroupBuyActivityEntity activity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .status(ActivityStatusEnumVO.ABANDONED)
                .startTime(new Date(System.currentTimeMillis() - 86400000))
                .endTime(new Date(System.currentTimeMillis() + 86400000))
                .build();

        when(repository.queryGroupBuyActivityEntityByActivityId(anyLong())).thenReturn(activity);

        try {
            filter.apply(commandEntity, dynamicContext);
            fail("Expected AppException");
            // 断言异常类型为AppException，且异常码为E0101
        } catch (AppException e) {
                assertEquals("E0101", e.getCode());
        } catch (Exception e) {
            fail("Expected AppException");
        }
    }

    @Test
    public void test_activity_not_started() {
        GroupBuyActivityEntity activity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .status(ActivityStatusEnumVO.EFFECTIVE)
                // 模拟活动开始时间在1天后，结束时间在2天后
                .startTime(new Date(System.currentTimeMillis() + 86400000))
                .endTime(new Date(System.currentTimeMillis() + 172800000))
                .build();

        when(repository.queryGroupBuyActivityEntityByActivityId(anyLong())).thenReturn(activity);

        try {
            filter.apply(commandEntity, dynamicContext);
            fail("Expected AppException");
        } catch (AppException e) {
                assertEquals("E0102", e.getCode());
        } catch (Exception e) {
            fail("Expected AppException");
        }
    }

    @Test
    public void test_activity_already_ended() {
        GroupBuyActivityEntity activity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .status(ActivityStatusEnumVO.EFFECTIVE)
                // 模拟活动开始时间在2天前，结束时间在1天前
                .startTime(new Date(System.currentTimeMillis() - 172800000))
                .endTime(new Date(System.currentTimeMillis() - 86400000))
                .build();

        when(repository.queryGroupBuyActivityEntityByActivityId(anyLong())).thenReturn(activity);

        try {
            filter.apply(commandEntity, dynamicContext);
            fail("Expected AppException");
        } catch (AppException e) {
                assertEquals("E0102", e.getCode());
        } catch (Exception e) {
            fail("Expected AppException");
        }
    }

    @Test
    public void test_activity_at_exact_start_time() throws Exception {
        long now = System.currentTimeMillis();
        GroupBuyActivityEntity activity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .status(ActivityStatusEnumVO.EFFECTIVE)
                // 模拟活动开始时间在当前时间前1秒，结束时间在1天后
                .startTime(new Date(now - 1000))
                .endTime(new Date(now + 86400000))
                .build();

        when(repository.queryGroupBuyActivityEntityByActivityId(anyLong())).thenReturn(activity);
        // 模拟下一个过滤器返回空
        mockNextFilter();
        //3.执行测试
        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        assertNotNull(result);
    }

    @Test
    public void test_activity_at_exact_end_time() throws Exception {
        long now = System.currentTimeMillis();
        GroupBuyActivityEntity activity = GroupBuyActivityEntity.builder()
                .activityId(1001L)
                .status(ActivityStatusEnumVO.EFFECTIVE)
                // 模拟活动开始时间在当前时间前1天，结束时间在当前时间后1秒
                .startTime(new Date(now - 86400000))
                .endTime(new Date(now + 1000))
                .build();
        when(repository.queryGroupBuyActivityEntityByActivityId(anyLong())).thenReturn(activity);
        // 模拟下一个过滤器返回空
        mockNextFilter();
        //3.执行测试
        TradeLockRuleFilterBackEntity result = filter.apply(commandEntity, dynamicContext);

        assertNotNull(result);
    }


}