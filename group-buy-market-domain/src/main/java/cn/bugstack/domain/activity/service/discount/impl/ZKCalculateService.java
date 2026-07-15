package cn.bugstack.domain.activity.service.discount.impl;

import cn.bugstack.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import cn.bugstack.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 折扣优惠计算
 * @create 2024-12-22 12:12
 */
@Slf4j
@Service("ZK")
public class ZKCalculateService extends AbstractDiscountCalculateService {

    @Override
    public BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        log.info("优惠策略折扣计算:{}", groupBuyDiscount.getDiscountType().getCode());

        String marketExpr = groupBuyDiscount.getMarketExpr();

        // 1. 空值保护
        if (StringUtils.isBlank(marketExpr)) {
            log.error("折扣优惠计算：marketExpr为空");
            return originalPrice;
        }

        // 2. 解析折扣率
        BigDecimal discountRate;
        try {
            discountRate = new BigDecimal(marketExpr);
        } catch (NumberFormatException e) {
            log.error("折扣优惠计算：marketExpr格式错误:{}", marketExpr);
            return originalPrice;
        }

        // 3. 负数校验
        if (discountRate.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("折扣优惠计算：折扣率无效({})，返回原价", discountRate);
            return originalPrice;
        }

        // 4. 计算折扣价
        BigDecimal deductionPrice = originalPrice.multiply(discountRate);

        // 5. 最低支付保护
        if (deductionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        return deductionPrice;
    }

}