package com.xk.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class SentinelGatewayConfig {

	private final List<ViewResolver> viewResolvers;
	private final ServerCodecConfigurer serverCodecConfigurer;

	public SentinelGatewayConfig(ObjectProvider<List<ViewResolver>> viewResolversProvider,
								 ServerCodecConfigurer serverCodecConfigurer) {
		this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
		this.serverCodecConfigurer = serverCodecConfigurer;
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
		return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
	}

	@PostConstruct
	public void doInit() {
		initCustomizedApis();
		initGatewayRules();
	}

	private void initCustomizedApis() {
		Set<ApiDefinition> definitions = new HashSet<>();

		ApiDefinition loginApi = new ApiDefinition("login-api")
				.setPredicateItems(new HashSet<ApiPredicateItem>() {{
					add(new ApiPathPredicateItem().setPattern("/api/login/**"));
				}});

		ApiDefinition adminApi = new ApiDefinition("admin-api")
				.setPredicateItems(new HashSet<ApiPredicateItem>() {{
					add(new ApiPathPredicateItem().setPattern("/api/admin/**"));
				}});

		ApiDefinition memberApi = new ApiDefinition("member-api")
				.setPredicateItems(new HashSet<ApiPredicateItem>() {{
					add(new ApiPathPredicateItem().setPattern("/api/member/**"));
				}});

		ApiDefinition employeeApi = new ApiDefinition("employee-api")
				.setPredicateItems(new HashSet<ApiPredicateItem>() {{
					add(new ApiPathPredicateItem().setPattern("/api/employee/**"));
				}});

		ApiDefinition equipmentApi = new ApiDefinition("equipment-api")
				.setPredicateItems(new HashSet<ApiPredicateItem>() {{
					add(new ApiPathPredicateItem().setPattern("/api/equipment/**"));
				}});

		ApiDefinition classApi = new ApiDefinition("class-api")
				.setPredicateItems(new HashSet<ApiPredicateItem>() {{
					add(new ApiPathPredicateItem().setPattern("/api/class/**"));
				}});

		ApiDefinition orderApi = new ApiDefinition("order-api")
				.setPredicateItems(new HashSet<ApiPredicateItem>() {{
					add(new ApiPathPredicateItem().setPattern("/api/order/**"));
				}});

		definitions.add(loginApi);
		definitions.add(adminApi);
		definitions.add(memberApi);
		definitions.add(employeeApi);
		definitions.add(equipmentApi);
		definitions.add(classApi);
		definitions.add(orderApi);

		GatewayApiDefinitionManager.loadApiDefinitions(definitions);
	}

	private void initGatewayRules() {
		Set<GatewayFlowRule> rules = new HashSet<>();

		rules.add(new GatewayFlowRule("login-api")
				.setCount(10)
				.setIntervalSec(1)
				.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME));

		rules.add(new GatewayFlowRule("admin-api")
				.setCount(20)
				.setIntervalSec(1)
				.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME));

		rules.add(new GatewayFlowRule("member-api")
				.setCount(50)
				.setIntervalSec(1)
				.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME));

		rules.add(new GatewayFlowRule("employee-api")
				.setCount(20)
				.setIntervalSec(1)
				.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME));

		rules.add(new GatewayFlowRule("equipment-api")
				.setCount(30)
				.setIntervalSec(1)
				.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME));

		rules.add(new GatewayFlowRule("class-api")
				.setCount(30)
				.setIntervalSec(1)
				.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME));

		rules.add(new GatewayFlowRule("order-api")
				.setCount(30)
				.setIntervalSec(1)
				.setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME));

		GatewayRuleManager.loadRules(rules);
	}
}
