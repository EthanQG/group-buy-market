package cn.bugstack.domain.trade.adapter.repository;

import cn.bugstack.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import cn.bugstack.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import cn.bugstack.domain.trade.model.entity.GroupBuyActivityEntity;
import cn.bugstack.domain.trade.model.entity.GroupBuyTeamEntity;
import cn.bugstack.domain.trade.model.entity.MarketPayOrderEntity;
import cn.bugstack.domain.trade.model.entity.NotifyTaskEntity;
import cn.bugstack.domain.trade.model.valobj.GroupBuyProgressVO;

import java.util.List;

/**
 * @author guigui
 * @date 2025/12/16 21:00
 * @description:交易仓储服务接口
 */
public interface ITradeRepository {

    /**
     * 查询，未支付的营销优惠订单
     *
     * @param userId     用户ID
     * @param outTradeNo 外部唯一单号
     * @return 拼团，预购订单营销实体对象
     */
    MarketPayOrderEntity queryMarketPayOrderEntityByOutTradeNo(String userId, String outTradeNo);
    /**
     * 锁定，营销预支付订单；商品下单前，预购锁定。
     *
     * @param groupBuyOrderAggregate 拼团订单聚合根对象
     * @return 拼团，预购订单营销实体对象
     */
    MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate);
    /**
     * 查询拼团进度
     *
     * @param teamId 拼团ID
     * @return 进度
     */
    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    /**
     * 查询用户在活动中已拼单数量
     *
     * @param activityId 活动ID
     * @param userId     用户ID
     * @return 已拼单数量
     */
    Integer queryOrderCountByActivityId(Long activityId, String userId);

    /**
     * 查询活动实体对象
     *
     * @param activityId 活动ID
     * @return 活动实体对象
     */
    GroupBuyActivityEntity queryGroupBuyActivityEntityByActivityId(Long activityId);

    /**
     * 通过拼团ID查询拼团订单实体对象
     *
     * @param teamId 拼团ID
     * @return 拼团订单实体对象
     */
    GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId);

     /**
     * 结算，营销预支付订单；商品下单后，预购结算。
     *
     * @param groupBuyTeamSettlementAggregate 拼团订单聚合根对象
     */
     NotifyTaskEntity settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate);

     /**
     * 查询渠道是否被拦截
     *
     * @param source  渠道来源
     * @param channel 渠道名称
     * @return 是否被拦截
     */
    boolean isSCBlackIntercept(String source, String channel);

    /**
     * 查询未执行的回调任务列表
     *
     * @return 未执行的回调任务列表
     */
    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList();

    /**
     * 查询未执行的回调任务列表
     *
     * @param teamId 拼团ID
     * @return 未执行的回调任务列表
     */
    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId);
    /**
     * 更新回调任务状态为成功
     *
     * @param teamId 拼团ID
     * @return 更新成功的记录数
     */
    int updateNotifyTaskStatusSuccess(String teamId);

     /**
     * 更新回调任务状态为失败
     *
     * @param teamId 拼团ID
     * @return 更新成功的记录数
     */
    int updateNotifyTaskStatusError(String teamId);

     /**
     * 更新回调任务状态为重试
     *
     * @param teamId 拼团ID
     * @return 更新成功的记录数
     */
    int updateNotifyTaskStatusRetry(String teamId);

    /**
     * 占用库存
     *
     * @param teamStockKey      库存键
     * @param recoveryTeamStockKey 库存恢复键
     * @param target            目标库存
     * @param validTime         库存有效时间
     * @return 是否占用成功
     */
    boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime);


    void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime);
}
