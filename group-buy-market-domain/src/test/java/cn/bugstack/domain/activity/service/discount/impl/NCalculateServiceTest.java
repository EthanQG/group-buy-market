package cn.bugstack.domain.activity.service.discount.impl;

import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class NCalculateServiceTest {

    private NCalculateService nCalculateService;

    @Before
    public void setUp() {
        nCalculateService = new NCalculateService();
    }

    // ==================== 等价类-正常场景 ====================

    /**
     * N-001：正常N元购 - 原价100元，9.9元购
     */
    @Test
    public void test_calculate_normal_n_price() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("9.9")
                .build();

        BigDecimal result = nCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertTrue("期望9.90，实际" + result, new BigDecimal("9.90").compareTo(result) == 0);
    }

    /**
     * N-002：原价略高于N元 - 原价10元，9.9元购
     */
    @Test
    public void test_calculate_price_slightly_higher_than_n() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("9.9")
                .build();

        BigDecimal result = nCalculateService.doCalculate(new BigDecimal("10.00"), discount);

        assertTrue("期望9.90，实际" + result, new BigDecimal("9.90").compareTo(result) == 0);
    }

    /**
     * N-005：大额差价 - 原价9999.99元，199元购
     */
    @Test
    public void test_calculate_large_price_difference() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("199")
                .build();

        BigDecimal result = nCalculateService.doCalculate(new BigDecimal("9999.99"), discount);

        assertTrue("期望199.00，实际" + result, new BigDecimal("199.00").compareTo(result) == 0);
    }

    // ==================== 边界值测试 ====================

    /**
     * N-003：原价等于N元 - 原价9.9元，9.9元购
     */
    @Test
    public void test_calculate_price_equal_to_n() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("9.9")
                .build();

        BigDecimal result = nCalculateService.doCalculate(new BigDecimal("9.90"), discount);

        assertTrue("期望9.90，实际" + result, new BigDecimal("9.90").compareTo(result) == 0);
      }

    /**
     * N-004：原价低于N元 - 原价5元，9.9元购
     */
    @Test
    public void test_calculate_price_lower_than_n() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("9.9")
                .build();

        BigDecimal result = nCalculateService.doCalculate(new BigDecimal("5.00"), discount);

        assertTrue("期望9.90，实际" + result, new BigDecimal("9.90").compareTo(result) == 0);
    }



    /**
     * N-006：最小金额 - 原价0.01元，9.9元购
     */
    @Test
    public void test_calculate_minimum_price() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("9.9")
                .build();

        BigDecimal result = nCalculateService.doCalculate(new BigDecimal("0.01"), discount);

        assertTrue("期望9.90，实际" + result, new BigDecimal("9.90").compareTo(result) == 0);
    }

    /**
     * N-008：N为零 - 返回原价
     */
    @Test
    public void test_calculate_n_zero_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("0")
                .build();

        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = nCalculateService.doCalculate(originalPrice, discount);

        assertEquals(originalPrice, result);
    }

    /**
     * N-009：N为负数 - 返回原价
     */
    @Test
    public void test_calculate_n_negative_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("-1")
                .build();

        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = nCalculateService.doCalculate(originalPrice, discount);

        assertEquals(originalPrice, result);
    }

    // ==================== 异常场景 ====================

    /**
     * N-007：marketExpr为空 - 返回原价
     */
    @Test
    public void test_calculate_null_expr_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr(null)
                .build();

        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = nCalculateService.doCalculate(originalPrice, discount);

        assertEquals(originalPrice, result);
    }

    @Test
    public void test_calculate_empty_expr_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("")
                .build();

        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = nCalculateService.doCalculate(originalPrice, discount);

        assertEquals(originalPrice, result);
    }

    @Test
    public void test_calculate_invalid_expr_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("invalid")
                .build();

        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = nCalculateService.doCalculate(originalPrice, discount);

        assertEquals(originalPrice, result);
    }

    // ==================== 最低值保护测试 ====================

    @Test
    public void test_calculate_minimum_price_protection() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("0.001")  // 小于0.01
                .build();

        BigDecimal result = nCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertEquals(new BigDecimal("0.01"), result);  // 最低保护价
    }
}