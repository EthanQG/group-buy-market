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
public class ZKCalculateServiceTest {

    private ZKCalculateService zkCalculateService;

    @Before
    public void setUp() {
        zkCalculateService = new ZKCalculateService();
    }

    @Test
    public void test_calculate_discount_success() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("0.8")
                .build();

        BigDecimal result = zkCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertEquals(new BigDecimal("80.00"), result);
    }

    @Test
    public void test_calculate_full_discount() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("1")
                .build();

        BigDecimal result = zkCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    public void test_calculate_minimum_discount() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("0")
                .build();

        BigDecimal result = zkCalculateService.doCalculate(new BigDecimal("100.00"), discount);

        assertEquals(new BigDecimal("0.01"), result);
    }

    @Test
    public void test_calculate_decimal_discount() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr("0.75")
                .build();

        assertEquals(new BigDecimal("74.99"), zkCalculateService.doCalculate(new BigDecimal("99.99"), discount));
    }

    @Test
    public void test_calculate_null_expr_returns_original(){
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(cn.bugstack.domain.activity.model.valobj.DiscountTypeEnum.BASE)
                .marketExpr(null)
                .build();

        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = zkCalculateService.doCalculate(originalPrice, discount);

        assertEquals(originalPrice, result);
    }

    @Test
    public void test_calculate_empty_expr_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("")  // 空字符串
                .build();
        
        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = zkCalculateService.doCalculate(originalPrice, discount);
        
        assertEquals(originalPrice, result);
    }

    @Test
    public void test_calculate_invalid_expr_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("abc")  // 非法格式
                .build();
        
        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = zkCalculateService.doCalculate(originalPrice, discount);
        
        // 格式错误返回原价
        assertEquals(originalPrice, result);
    }

    @Test
    public void test_calculate_negative_discount_returns_original() {
        GroupBuyActivityDiscountVO.GroupBuyDiscount discount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountType(DiscountTypeEnum.BASE)
                .marketExpr("-0.5")  // 负数折扣
                .build();
        
        BigDecimal originalPrice = new BigDecimal("100.00");
        BigDecimal result = zkCalculateService.doCalculate(originalPrice, discount);
        
        // 负数折扣不生效，返回原价
        assertEquals(originalPrice, result);
    }
}