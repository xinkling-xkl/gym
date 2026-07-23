package org.example.login.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
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

		FlowRule loginFlowRule = new FlowRule();
		loginFlowRule.setResource("login");
		loginFlowRule.setCount(10);
		loginFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		loginFlowRule.setLimitApp("default");
		rules.add(loginFlowRule);

		FlowRule adminGetRule = new FlowRule();
		adminGetRule.setResource("login-admin-get");
		adminGetRule.setCount(20);
		adminGetRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		adminGetRule.setLimitApp("default");
		rules.add(adminGetRule);

		FlowRule memberGetRule = new FlowRule();
		memberGetRule.setResource("login-member-get");
		memberGetRule.setCount(50);
		memberGetRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
		memberGetRule.setLimitApp("default");
		rules.add(memberGetRule);

		FlowRuleManager.loadRules(rules);
	}
}
