package cn.bugstack.domain.activity.service.discount.impl;

import cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum;
import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ZJCalculateServiceTest {

    private ZJCalculateService zjCalculateService;

    @Before
    public void setUp() {
        zjCalculateService = new ZJCalculateService();
    }

    @Test
    public void test_calculate_direct_reduction_success() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("20")
                .build();

        BigDecimal result = zjCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertEquals(new BigDecimal("80.00"), result);
    }

    @Test
    public void test_calculate_direct_reduction_at_threshold() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("100")
                .build();

        BigDecimal result = zjCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertEquals(new BigDecimal("0.01"), result);
    }

    @Test
    public void test_calculate_direct_reduction_exceeds_price() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("150")
                .build();

        BigDecimal result = zjCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertEquals(new BigDecimal("0.01"), result);
    }

    @Test
    public void test_calculate_decimal_direct_reduction() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("10.50")
                .build();
        BigDecimal result = zjCalculateService.doCalculate(new BigDecimal("99.99"), discount);

        assertEquals("89.49", String.valueOf(result));
    }

    @Test
    public void test_calculate_null_expr_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr(null)
                .build();
        
        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = zjCalculateService.doCalculate(originalPrice, discount);
        
        assertTrue("期望原价" + originalPrice + "，实际" + result, 
                originalPrice.compareTo(result) == 0);  // null返回原价
    }

    @Test
    public void test_calculate_empty_expr_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("")
                .build();
        
        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = zjCalculateService.doCalculate(originalPrice, discount);
        
        assertEquals(originalPrice, result);  // 空字符串返回原价
    }

    @Test
    public void test_calculate_invalid_expr_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("abc")
                .build();
        
        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = zjCalculateService.doCalculate(originalPrice, discount);
        
        assertEquals(originalPrice, result);  // 非法格式返回原价
    }

    @Test
    public void test_calculate_negative_reduction_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("-20")  // 负数直减
                .build();
        
        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = zjCalculateService.doCalculate(originalPrice, discount);
        
        // 负数直减不生效，返回原价（100 - (-20) = 120，但这不是优惠）
        assertEquals(originalPrice, result);
    }
}