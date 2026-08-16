package org.example.plan.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 限流配置
 * 健身计划接口限流策略：
 *   - 列表查询：50 QPS
 *   - 单条查询/状态切换：30 QPS
 *   - 新增/更新：10 QPS
 *   - 删除：5 QPS
 */
@Configuration
public class SentinelConfig {

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 列表查询 50 QPS
        rules.add(buildRule("plan-list", 50));
        // 单条查询 30 QPS
        rules.add(buildRule("plan-get", 30));
        // 新增计划 10 QPS
        rules.add(buildRule("plan-add", 10));
        // 更新计划 10 QPS
        rules.add(buildRule("plan-update", 10));
        // 状态切换 30 QPS
        rules.add(buildRule("plan-status", 30));
        // 删除计划 5 QPS
        rules.add(buildRule("plan-delete", 5));
        // 训练项新增 10 QPS
        rules.add(buildRule("plan-item-add", 10));
        // 训练项更新 10 QPS
        rules.add(buildRule("plan-item-update", 10));
        // 训练项删除 5 QPS
        rules.add(buildRule("plan-item-delete", 5));
        // 训练项完成状态切换 30 QPS
        rules.add(buildRule("plan-item-toggle", 30));
        // 同步课程订单 5 QPS（涉及跨服务调用，限制更严）
        rules.add(buildRule("plan-sync", 5));

        FlowRuleManager.loadRules(rules);
    }

    private FlowRule buildRule(String resource, int qps) {
        FlowRule rule = new FlowRule();
        rule.setResource(resource);
        rule.setCount(qps);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setLimitApp("default");
        return rule;
    }
}
