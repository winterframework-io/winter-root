/*
 * Copyright 2019 Jeremy KUHN
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
package io.winterframework.test.provide.moduleC;

import java.util.concurrent.Callable;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Provide;
import io.winterframework.core.annotation.Wrapper;

import java.util.function.Supplier;

@Bean
@Wrapper
public class BeanB implements Supplier<Callable<String>>, @Provide Runnable {

	public Callable<String> get() {
		return null;
	}
	
	@Override
	public void run() {
		
	}
}
