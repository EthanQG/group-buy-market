package cn.bugstack.domain.activity.service.discount.impl;

import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.bugstack.types.common.Constants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class MJCalculateServiceTest {

    private MJCalculateService mjCalculateService;

    @Before
    public void setUp() {
        mjCalculateService = new MJCalculateService();
    }

    @Test
    public void test_calculate_full_reduction_success() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("100" + Constants.SPLIT + "10")
                .build();

        BigDecimal result = mjCalculateService.doCalculate(new BigDecimal("150.00"), discount);

        assertEquals(new BigDecimal("140.00"), result);
    }

    @Test
    public void test_calculate_at_threshold() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("100" + Constants.SPLIT + "10")
                .build();

        BigDecimal result = mjCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertEquals(new BigDecimal("90.00"), result);
    }

    @Test
    public void test_calculate_below_threshold_return_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("100" + Constants.SPLIT + "10")
                .build();

        BigDecimal result = mjCalculateService.doCalculate(new BigDecimal("99.99"), discount);

        assertEquals(new BigDecimal("99.99"), result);
    }

    @Test
    public void test_calculate_deduction_below_zero_return_minimum() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("50" + Constants.SPLIT + "100")
                .build();

        BigDecimal result = mjCalculateService.doCalculate(new BigDecimal("50.00"), discount);

        assertEquals(new BigDecimal("0.01"), result);
    }

    @Test
    public void test_calculate_empty_marketExpr_return_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("")
                .build();

        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = mjCalculateService.doCalculate(originalPrice, discount);

        assertEquals(originalPrice, result);
    }

    @Test
    public void test_calculate_null_marketExpr_return_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr(null)
                .build();

        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = mjCalculateService.doCalculate(originalPrice, discount);

        assertEquals(originalPrice, result);
    }

    @Test
    public void test_calculate_boundary_value_exactly_threshold() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("100" + Constants.SPLIT + "0.01")
                .build();

        BigDecimal result = mjCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertEquals(new BigDecimal("99.99"), result);
    }

    @Test
    public void test_calculate_large_amount() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("1000" + Constants.SPLIT + "100")
                .build();

        BigDecimal result = mjCalculateService.doCalculate(new BigDecimal("9999.99"), discount);

        assertEquals(new BigDecimal("9899.99"), result);
    }

    @Test
    public void test_calculate_small_amount() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("1" + Constants.SPLIT + "0.5")
                .build();

        BigDecimal result = mjCalculateService.doCalculate(new BigDecimal("1.00"), discount);

        assertEquals(new BigDecimal("0.50"), result);
    }

    @Test
    public void test_calculate_decimal_values() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("99.99" + Constants.SPLIT + "10.50")
                .build();

        BigDecimal result = mjCalculateService.doCalculate(new BigDecimal("199.99"), discount);

        assertEquals(new BigDecimal("189.49"), result);
    }
}