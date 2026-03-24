package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.GroupBuyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * @author guigui
 * @date 2025/12/16 20:13
 * @description: 用户拼团
 */
@Mapper
public interface IGroupBuyOrderDao {

    //插入用户拼团订单
    void insert(GroupBuyOrder groupBuyOrder);

    //更新用户拼团订单锁单数量
    int updateAddLockCount(String teamId);

    //更新用户拼团订单锁单数量
    int updateSubtractionLockCount(String teamId);

    //查询用户拼团订单进度
    GroupBuyOrder queryGroupBuyProgress(String teamId);

    //通过拼团ID查询拼团订单实体对象
    GroupBuyOrder queryGroupBuyTeamByTeamId(String teamId);

    //更新用户拼团订单完成数量
    int updateAddCompleteCount(String teamId);

    //更新用户拼团订单状态为完成
    int updateOrderStatus2COMPLETE(String teamId);

    //根据拼团ID列表查询用户拼团订单进度
    List<GroupBuyOrder> queryGroupBuyProgressByTeamIds(@Param("teamIds") Set<String> teamIds);

    //根据拼团ID列表查询所有拼团数量
    Integer queryAllTeamCount(@Param("teamIds")Set<String> teamIds);

    //根据拼团ID列表查询所有拼团完成数量
    Integer queryAllTeamCompleteCount(@Param("teamIds")Set<String> teamIds);

    //根据拼团ID列表查询所有拼团用户数量
    Integer queryAllUserCount(@Param("teamIds")Set<String> teamIds);
}
