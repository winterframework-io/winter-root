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
package io.winterframework.core.compiler.module;

import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import io.winterframework.core.annotation.Module;
import io.winterframework.core.compiler.ModuleAnnotationProcessor;
import io.winterframework.core.compiler.spi.ModuleInfoBuilder;
import io.winterframework.core.compiler.spi.ModuleQualifiedName;

/**
 * <p>
 * Base class for module info builders.
 * </p>
 * 
 * @author jkuhn
 *
 */
abstract class AbstractModuleInfoBuilder implements ModuleInfoBuilder {

	protected ProcessingEnvironment processingEnvironment;
	
	protected ModuleElement moduleElement;
	
	protected AnnotationMirror moduleAnnotation;

	protected ModuleQualifiedName moduleQName;
	
	protected int version;
	
	public AbstractModuleInfoBuilder(ProcessingEnvironment processingEnvironment, ModuleElement moduleElement) {
		this.processingEnvironment = processingEnvironment;
		this.moduleElement = moduleElement;
		
		TypeMirror moduleAnnotationType = this.processingEnvironment.getElementUtils().getTypeElement(Module.class.getCanonicalName()).asType();
		Optional<? extends AnnotationMirror> moduleAnnotation = this.processingEnvironment.getElementUtils().getAllAnnotationMirrors(moduleElement).stream().filter(a -> this.processingEnvironment.getTypeUtils().isSameType(a.getAnnotationType(), moduleAnnotationType)).findFirst();
		if(!moduleAnnotation.isPresent()) {
			throw new IllegalArgumentException("The specified element is not annotated with " + Module.class.getSimpleName());
		}
		
		this.moduleAnnotation = moduleAnnotation.get();
		
		String packageName = "";
		String moduleName = "";
		String moduleClassName = null;
		
		for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.processingEnvironment.getElementUtils().getElementValuesWithDefaults(this.moduleAnnotation).entrySet()) {
			switch(value.getKey().getSimpleName().toString()) {
				case "className" : moduleClassName = (String)value.getValue().getValue();
					break;
			}
		}
		
		if(moduleElement.isUnnamed()) {
			moduleName = "DefaultModule";
		}
		else {
			packageName = this.moduleElement.getQualifiedName().toString();
			packageName = packageName.substring(0, packageName.lastIndexOf("."));
			
			moduleName = this.moduleElement.getQualifiedName().toString().substring(packageName.length() + 1);
		}
		
		this.moduleQName = new ModuleQualifiedName(packageName, moduleName, moduleClassName);
		
		TypeElement moduleType = this.processingEnvironment.getElementUtils().getTypeElement(this.moduleQName.getClassName());
		
		if(moduleType != null) {
			String name = this.processingEnvironment.getTypeUtils().asElement(moduleType.getSuperclass()).toString();
			if(name.equals("io.winterframework.core.v1.Module")) {
				this.version = 1;
			}
		}
		else {
			this.version = ModuleAnnotationProcessor.VERSION;
		}
	}
}
