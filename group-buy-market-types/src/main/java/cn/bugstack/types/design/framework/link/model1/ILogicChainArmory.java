package cn.bugstack.types.design.framework.link.model1;

/**
 * @author guigui
 * @date 2025/12/20 16:45
 * @description:责任链装配
 */
public interface ILogicChainArmory<T, D, R> {

    ILogicLink<T, D, R> next();

    ILogicLink<T, D, R> appendNext(ILogicLink<T, D, R> next);

}
