package com.gym.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelConfig {

	@Bean
	public SentinelResourceAspect sentinelResourceAspect() {
		return new SentinelResourceAspect();
	}

	@PostConstruct
	public void initFlowRules() {
		List<FlowRule> rules = new ArrayList<>();

		FlowRule adminFlowRule = new FlowRule();
		adminFlowRule.setResource("admin-get");
		adminFlowRule.setCount(20);
		adminFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		adminFlowRule.setLimitApp("default");
		rules.add(adminFlowRule);

		FlowRule memberFlowRule = new FlowRule();
		memberFlowRule.setResource("member-list");
		memberFlowRule.setCount(50);
		memberFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		memberFlowRule.setLimitApp("default");
		rules.add(memberFlowRule);

		FlowRule employeeFlowRule = new FlowRule();
		employeeFlowRule.setResource("employee-list");
		employeeFlowRule.setCount(20);
		employeeFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		employeeFlowRule.setLimitApp("default");
		rules.add(employeeFlowRule);

		FlowRule equipmentFlowRule = new FlowRule();
		equipmentFlowRule.setResource("equipment-list");
		equipmentFlowRule.setCount(30);
		equipmentFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		equipmentFlowRule.setLimitApp("default");
		rules.add(equipmentFlowRule);

		FlowRule classFlowRule = new FlowRule();
		classFlowRule.setResource("class-list");
		classFlowRule.setCount(30);
		classFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		classFlowRule.setLimitApp("default");
		rules.add(classFlowRule);

		FlowRule orderFlowRule = new FlowRule();
		orderFlowRule.setResource("order-list");
		orderFlowRule.setCount(30);
		orderFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		orderFlowRule.setLimitApp("default");
		rules.add(orderFlowRule);

		FlowRule memberGetRule = new FlowRule();
		memberGetRule.setResource("member-get");
		memberGetRule.setCount(30);
		memberGetRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		memberGetRule.setLimitApp("default");
		rules.add(memberGetRule);

		FlowRule adminAddRule = new FlowRule();
		adminAddRule.setResource("admin-add");
		adminAddRule.setCount(5);
		adminAddRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		adminAddRule.setLimitApp("default");
		rules.add(adminAddRule);

		FlowRule memberAddRule = new FlowRule();
		memberAddRule.setResource("member-add");
		memberAddRule.setCount(10);
		memberAddRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		memberAddRule.setLimitApp("default");
		rules.add(memberAddRule);

		FlowRule memberUpdateRule = new FlowRule();
		memberUpdateRule.setResource("member-update");
		memberUpdateRule.setCount(10);
		memberUpdateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		memberUpdateRule.setLimitApp("default");
		rules.add(memberUpdateRule);

		FlowRule memberDeleteRule = new FlowRule();
		memberDeleteRule.setResource("member-delete");
		memberDeleteRule.setCount(5);
		memberDeleteRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		memberDeleteRule.setLimitApp("default");
		rules.add(memberDeleteRule);

		FlowRule equipmentAddRule = new FlowRule();
		equipmentAddRule.setResource("equipment-add");
		equipmentAddRule.setCount(5);
		equipmentAddRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		equipmentAddRule.setLimitApp("default");
		rules.add(equipmentAddRule);

		FlowRule equipmentUpdateRule = new FlowRule();
		equipmentUpdateRule.setResource("equipment-update");
		equipmentUpdateRule.setCount(5);
		equipmentUpdateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		equipmentUpdateRule.setLimitApp("default");
		rules.add(equipmentUpdateRule);

		FlowRule equipmentDeleteRule = new FlowRule();
		equipmentDeleteRule.setResource("equipment-delete");
		equipmentDeleteRule.setCount(5);
		equipmentDeleteRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		equipmentDeleteRule.setLimitApp("default");
		rules.add(equipmentDeleteRule);

		FlowRule classAddRule = new FlowRule();
		classAddRule.setResource("class-add");
		classAddRule.setCount(5);
		classAddRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		classAddRule.setLimitApp("default");
		rules.add(classAddRule);

		FlowRule classUpdateRule = new FlowRule();
		classUpdateRule.setResource("class-update");
		classUpdateRule.setCount(5);
		classUpdateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		classUpdateRule.setLimitApp("default");
		rules.add(classUpdateRule);

		FlowRule classDeleteRule = new FlowRule();
		classDeleteRule.setResource("class-delete");
		classDeleteRule.setCount(5);
		classDeleteRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		classDeleteRule.setLimitApp("default");
		rules.add(classDeleteRule);

		FlowRule orderAddRule = new FlowRule();
		orderAddRule.setResource("order-add");
		orderAddRule.setCount(10);
		orderAddRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		orderAddRule.setLimitApp("default");
		rules.add(orderAddRule);

		FlowRule orderUpdateRule = new FlowRule();
		orderUpdateRule.setResource("order-update");
		orderUpdateRule.setCount(10);
		orderUpdateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		orderUpdateRule.setLimitApp("default");
		rules.add(orderUpdateRule);

		FlowRule orderDeleteRule = new FlowRule();
		orderDeleteRule.setResource("order-delete");
		orderDeleteRule.setCount(5);
		orderDeleteRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		orderDeleteRule.setLimitApp("default");
		rules.add(orderDeleteRule);

		FlowRule employeeAddRule = new FlowRule();
		employeeAddRule.setResource("employee-add");
		employeeAddRule.setCount(5);
		employeeAddRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		employeeAddRule.setLimitApp("default");
		rules.add(employeeAddRule);

		FlowRule employeeUpdateRule = new FlowRule();
		employeeUpdateRule.setResource("employee-update");
		employeeUpdateRule.setCount(5);
		employeeUpdateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		employeeUpdateRule.setLimitApp("default");
		rules.add(employeeUpdateRule);

		FlowRule employeeDeleteRule = new FlowRule();
		employeeDeleteRule.setResource("employee-delete");
		employeeDeleteRule.setCount(5);
		employeeDeleteRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		employeeDeleteRule.setLimitApp("default");
		rules.add(employeeDeleteRule);

		FlowRuleManager.loadRules(rules);
	}

	public static String blockHandler(String resource, BlockException ex) {
		return "{\"code\": 429, \"message\": \"请求过于频繁，请稍后再试\", \"data\": null}";
	}
}
