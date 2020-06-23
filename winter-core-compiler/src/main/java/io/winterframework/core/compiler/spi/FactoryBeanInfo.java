/*
 * Copyright 2018 Jeremy KUHN
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
package io.winterframework.core.compiler.spi;

import javax.lang.model.type.TypeMirror;

/**
 * <p>
 * A factory bean info holds the data required to process a factory bean in a
 * module.
 * </p>
 * 
 * @author jkuhn
 *
 */
public interface FactoryBeanInfo extends ModuleBeanInfo {

	/**
	 * <p>
	 * Returns the factory type which is the type of the class supplying the actual
	 * bean whose type is given by {@link BeanInfo#getType()}.
	 * </p>
	 * 
	 * @return a type
	 */
	TypeMirror getFactoryType();
}