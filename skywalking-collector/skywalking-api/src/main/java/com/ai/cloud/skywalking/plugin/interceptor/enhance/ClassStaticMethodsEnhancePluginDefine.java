package com.ai.cloud.skywalking.plugin.interceptor.enhance;

import com.ai.cloud.skywalking.plugin.interceptor.MethodMatcher;

/**
 * 仅增强拦截类级别静态方法
 * 
 * @author wusheng
 *
 */
public abstract class ClassStaticMethodsEnhancePluginDefine extends
		ClassEnhancePluginDefine {

	@Override
	protected MethodMatcher[] getInstanceMethodsMatchers() {
		return null;
	}

	@Override
	protected IntanceMethodsAroundInterceptor getInstanceMethodsInterceptor() {
		return null;
	}
}
