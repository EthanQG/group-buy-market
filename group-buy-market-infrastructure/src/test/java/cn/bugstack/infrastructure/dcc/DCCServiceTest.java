package cn.bugstack.infrastructure.dcc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class DCCServiceTest {

    private DCCService dccService;

    @Before
    public void setUp() {
        dccService = new DCCService();
    }

    // ========== DCC-001: 降级开关测试 ==========
    // 测试目标：验证降级开关功能是否正常工作
    // 业务场景：当系统出现异常时，可以通过降级开关关闭某些功能，保证核心业务可用

    @Test
    public void test_is_downgrade_switch_off() {
        // 测试场景：降级开关关闭状态
        // 输入：downgradeSwitch = "0"
        // 预期结果：isDowngradeSwitch() 返回 false，表示降级功能未开启
        // 测试目的：验证开关关闭时，系统正常提供服务
        ReflectionTestUtils.setField(dccService, "downgradeSwitch", "0");
        assertFalse(dccService.isDowngradeSwitch());
    }

    @Test
    public void test_is_downgrade_switch_on() {
        // 测试场景：降级开关开启状态
        // 输入：downgradeSwitch = "1"
        // 预期结果：isDowngradeSwitch() 返回 true，表示降级功能已开启
        // 测试目的：验证开关开启时，系统进入降级模式
        ReflectionTestUtils.setField(dccService, "downgradeSwitch", "1");
        assertTrue(dccService.isDowngradeSwitch());
    }

    @Test
    public void test_is_downgrade_switch_invalid() {
        // 测试场景：降级开关配置了无效值
        // 输入：downgradeSwitch = "2"（非0和1的值）
        // 预期结果：isDowngradeSwitch() 返回 false，采用默认关闭策略
        // 测试目的：验证容错机制，防止配置错误导致系统异常
        ReflectionTestUtils.setField(dccService, "downgradeSwitch", "2");
        assertFalse(dccService.isDowngradeSwitch());
    }

    @Test
    public void test_is_downgrade_switch_null() {
        // 测试场景：降级开关配置为null（配置丢失）
        // 输入：downgradeSwitch = null
        // 预期结果：isDowngradeSwitch() 返回 false，采用默认关闭策略
        // 测试目的：验证空值保护机制，防止配置中心异常导致系统崩溃
        ReflectionTestUtils.setField(dccService, "downgradeSwitch", null);
        assertFalse(dccService.isDowngradeSwitch());
    }

    // ========== DCC-002: 切量范围测试 ==========
    // 测试目标：验证切量功能是否按照预期比例分配流量
    // 业务场景：灰度发布时，通过切量功能让部分用户使用新版本，逐步扩大范围

    @Test
    public void test_is_cut_range_within_range() {
        // 测试场景：正常切量范围（50%）
        // 输入：cutRange = "50"，userId = "user123"
        // 预期结果：isCutRange() 返回 true，表示该用户被选中进入切量范围
        // 测试目的：验证切量功能在正常比例下能否正确识别用户
        // 原理：通过 userId.hashCode() % 100 计算哈希值，判断是否在切量范围内
        ReflectionTestUtils.setField(dccService, "cutRange", "50");

        ReflectionTestUtils.setField(dccService, "downgradeSwitch", "1");
        assertTrue(dccService.isCutRange("user123"));
    }

    @Test
    public void test_is_cut_range_boundary_0() {
        // 测试场景：切量范围为0%（不切量）
        // 输入：cutRange = "0"
        // 预期结果：isCutRange() 返回 false，所有用户都不在切量范围内
        // 测试目的：验证边界值0的处理，确保不会误切量
        ReflectionTestUtils.setField(dccService, "cutRange", "0");

        assertFalse(dccService.isCutRange("user"));
    }

    @Test
    public void test_is_cut_range_boundary_100() {
        // 测试场景：切量范围为100%（全量切量）
        // 输入：cutRange = "100"
        // 预期结果：isCutRange() 返回 true，所有用户都在切量范围内
        // 测试目的：验证边界值100的处理，确保全量发布时所有用户都能被覆盖
        ReflectionTestUtils.setField(dccService, "cutRange", "100");

        assertTrue(dccService.isCutRange("user"));
    }

    @Test
    public void test_is_cut_range_user_hash_distribution() {
        // 测试场景：验证哈希分布的均匀性
        // 输入：cutRange = "30"，测试1000个不同的userId
        // 预期结果：约30%的用户在切量范围内（允许±5%误差）
        // 测试目的：验证哈希算法的均匀性，确保切量比例准确
        // 原理：通过大量测试验证 Hash 分布是否符合预期比例
        // 应用场景：灰度发布时需要准确控制流量比例，避免过多或过少用户被切量
        ReflectionTestUtils.setField(dccService, "cutRange", "30");

        int withinRange = 0;
        int totalTests = 1000;

        for (int i = 0; i < totalTests; i++) {
            String userId = "user_" + i;
            if (dccService.isCutRange(userId)) {
                withinRange++;
            }
        }

        double percentage = (withinRange * 100.0) / totalTests;
        assertTrue("Expected approximately 30% within range, got " + percentage + "%",
                percentage >= 25 && percentage <= 35);
    }

    // ========== DCC-003: 黑名单渠道测试 ==========
    // 测试目标：验证黑名单拦截功能是否正常工作
    // 业务场景：当某些渠道（source+channel）出现异常或需要限流时，可以通过黑名单拦截这些渠道的请求

    @Test
    public void test_is_sc_black_intercept_not_in_blacklist() {
        // 测试场景：请求来源不在黑名单中
        // 输入：scBlacklist = "s01c01,s02c02"，测试来源为 "s01"+"c03"、"s03"+"c01"、"s03"+"c03"
        // 预期结果：isSCBlackIntercept() 返回 false，表示请求被放行
        // 测试目的：验证不在黑名单中的渠道能够正常访问系统
        // 原理：通过 source+channel 拼接后判断是否在黑名单列表中
        ReflectionTestUtils.setField(dccService, "scBlacklist", "s01c01,s02c02");

        assertFalse(dccService.isSCBlackIntercept("s01", "c03"));
        assertFalse(dccService.isSCBlackIntercept("s03", "c01"));
        assertFalse(dccService.isSCBlackIntercept("s03", "c03"));
    }

    @Test
    public void test_is_sc_black_intercept_in_blacklist() {
        // 测试场景：请求来源在黑名单中
        // 输入：scBlacklist = "s01c01,s02c02,s03c03"，测试来源为 "s01"+"c01"、"s02"+"c02"、"s03"+"c03"
        // 预期结果：isSCBlackIntercept() 返回 true，表示请求被拦截
        // 测试目的：验证黑名单渠道能够正确拦截，防止异常流量进入系统
        // 应用场景：当某个渠道出现大量异常请求时，可以通过黑名单快速拦截，保护系统
        ReflectionTestUtils.setField(dccService, "scBlacklist", "s01c01,s02c02,s03c03");

        assertTrue(dccService.isSCBlackIntercept("s01", "c01"));
        assertTrue(dccService.isSCBlackIntercept("s02", "c02"));
        assertTrue(dccService.isSCBlackIntercept("s03", "c03"));
    }

    @Test
    public void test_is_sc_black_intercept_single_channel() {
        // 测试场景：黑名单中只有一个渠道
        // 输入：scBlacklist = "s01c01"，测试来源为 "s01"+"c01" 和 "s01"+"c02"
        // 预期结果：s01c01 返回 true（拦截），s01c02 返回 false（放行）
        // 测试目的：验证单个渠道的精确匹配，避免误杀其他渠道
        ReflectionTestUtils.setField(dccService, "scBlacklist", "s01c01");

        assertTrue(dccService.isSCBlackIntercept("s01", "c01"));
        assertFalse(dccService.isSCBlackIntercept("s01", "c02"));
    }

    @Test
    public void test_is_sc_black_intercept_whitespace_handling() {
        // 测试场景：黑名单配置中包含空格
        // 输入：scBlacklist = " s01c01 , s02c02 "（带空格）
        // 预期结果：能够正确识别 "s01c01" 和 "s02c02"，返回 true
        // 测试目的：验证配置格式容错，即使配置中包含空格也能正常工作
        // 应用场景：配置中心推送配置时可能包含空格，需要系统能够正确处理
        ReflectionTestUtils.setField(dccService, "scBlacklist", " s01c01 , s02c02 ");

        assertTrue(dccService.isSCBlackIntercept("s01", "c01"));
        assertTrue(dccService.isSCBlackIntercept("s02", "c02"));
    }

    // ========== DCC-004: 空配置保护测试 ==========
    // 测试目标：验证空配置的保护机制
    // 业务场景：当配置中心异常或配置丢失时，系统应该能够正常工作，不会因为配置缺失而崩溃

    @Test
    public void test_is_sc_black_intercept_empty_blacklist() {
        // 测试场景：黑名单配置为空字符串
        // 输入：scBlacklist = ""（空字符串）
        // 预期结果：isSCBlackIntercept() 返回 false，表示所有请求都被放行
        // 测试目的：验证空配置保护机制，防止配置丢失导致系统异常
        // 原理：当黑名单为空时，split(",") 会返回包含一个空字符串的列表，但不会匹配任何 source+channel
        // 应用场景：配置中心宕机或配置被误删除时，系统应该采用默认策略（放行所有请求），保证业务可用性
        ReflectionTestUtils.setField(dccService, "scBlacklist", "");

        assertFalse(dccService.isSCBlackIntercept("s01", "c01"));
    }
}