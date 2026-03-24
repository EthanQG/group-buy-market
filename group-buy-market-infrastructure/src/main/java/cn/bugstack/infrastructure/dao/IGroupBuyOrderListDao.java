package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.GroupBuyOrderList;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author guigui
 * @date 2025/12/16 20:21
 * @description:用户拼单明细
 */
@Mapper
public interface IGroupBuyOrderListDao {

    //插入用户拼单明细
    void insert(GroupBuyOrderList groupBuyOrderList);

    //查询交易记录 根据外部交易单号和用户ID查询
    GroupBuyOrderList queryGroupBuyOrderRecordByOutTradeNo(GroupBuyOrderList groupBuyOrderListReq);

    //根据用户ID和活动ID查询用户拼单数量
    Integer queryOrderCountByActivityId(GroupBuyOrderList groupBuyOrderListReq);

    //更新用户拼单订单状态为完成
    int updateOrderStatus2COMPLETE(GroupBuyOrderList groupBuyOrderListReq);

    //根据拼团ID查询用户拼单订单状态为完成的外部交易单号列表
    List<String> queryGroupBuyCompleteOrderOutTradeNoListByTeamId(String teamId);

    //根据用户ID查询进行中的拼团订单
    List<GroupBuyOrderList> queryInProgressUserGroupBuyOrderDetailListByUserId(GroupBuyOrderList groupBuyOrderListReq);

    //根据用户ID随机查询进行中的拼团订单
    List<GroupBuyOrderList> queryInProgressUserGroupBuyOrderDetailListByRandom(GroupBuyOrderList groupBuyOrderListReq);

    //根据活动ID查询进行中的拼团订单
    List<GroupBuyOrderList> queryInProgressUserGroupBuyOrderDetailListByActivityId(Long activityId);
}
