/*
 * Copyright 2020 Jeremy KUHN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.winterframework.test.lifecycle.moduleC;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Wrapper;
import io.winterframework.core.annotation.Bean.Strategy;
import io.winterframework.core.annotation.Destroy;
import io.winterframework.core.annotation.Init;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Bean(strategy=Strategy.SINGLETON)
@Wrapper
public class SingletonStrategyWrapperBean implements Supplier<BeanB> {

	private InjectedBean injectedBean;

	private BeanB instance;
	
	public SingletonStrategyWrapperBean(InjectedBean injectedBean) {
		this.injectedBean = injectedBean;
		
		this.instance = new BeanB(injectedBean);
	}
	
	public BeanB get() {
		return this.instance;
	}
	
	@Init
	public void init() {
		System.out.println("init singleton " + this.instance.hashCode());
		this.instance.init();
	}
	
	@Destroy
	public void destroy() {
		System.out.println("destroy singleton " + this.instance.hashCode());
		this.instance.destroy();
	}
}
