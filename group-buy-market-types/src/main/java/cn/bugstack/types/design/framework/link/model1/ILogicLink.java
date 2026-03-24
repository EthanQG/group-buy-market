package cn.bugstack.types.design.framework.link.model1;

/**
 * @author guigui
 * @date 2025/12/20 16:45
 * @description:策略规则责任链接口
 */
public interface ILogicLink<T, D, R> extends ILogicChainArmory<T, D, R> {

    R apply(T requestParameter, D dynamicContext) throws Exception;

}