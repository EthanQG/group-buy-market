package cn.bugstack.domain.activity.service.discount.impl;

import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.bugstack.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description N元购优惠计算
 * @create 2024-12-22 12:12
 */
@Slf4j
@Service("N")
public class NCalculateService extends AbstractDiscountCalculateService {

    private static final BigDecimal MINIMUM_PRICE = new BigDecimal("0.01");

    @Override
    public BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        log.info("优惠策略折扣计算:{}", groupBuyDiscount.getDiscountType().getCode());

        String marketExpr = groupBuyDiscount.getMarketExpr();
        
        // 空值处理
        if (StringUtils.isBlank(marketExpr)) {
            log.error("N元购优惠计算：marketExpr为空");
            return originalPrice;
        }

        BigDecimal nPrice;
        try {
            nPrice = new BigDecimal(marketExpr);
        } catch (NumberFormatException e) {
            log.error("N元购优惠计算：marketExpr格式错误:{}", marketExpr);
            return originalPrice;
        }

        // N值校验：小于等于0不生效
        if (nPrice.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("N元购优惠计算：N值无效({})，返回原价", nPrice);
            return originalPrice;
        }

        // N元购优惠价
        BigDecimal result = nPrice;

        // 最低支付保护：不能低于0.01元
        if (result.compareTo(MINIMUM_PRICE) < 0) {
            result = MINIMUM_PRICE;
        }

        return result;
    }

}
