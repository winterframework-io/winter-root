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
package io.winterframework.core.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;

import io.winterframework.core.annotation.Bean;
import io.winterframework.core.annotation.Configuration;
import io.winterframework.core.annotation.Module;
import io.winterframework.core.compiler.bean.BeanCompilationException;
import io.winterframework.core.compiler.bean.ModuleBeanInfoFactory;
import io.winterframework.core.compiler.configuration.ConfigurationInfoFactory;
import io.winterframework.core.compiler.module.ModuleInfoBuilderFactory;
import io.winterframework.core.compiler.module.ModuleMetadataExtractor;
import io.winterframework.core.compiler.socket.SocketBeanInfoFactory;
import io.winterframework.core.compiler.socket.SocketCompilationException;
import io.winterframework.core.compiler.spi.ConfigurationInfo;
import io.winterframework.core.compiler.spi.ModuleBeanInfo;
import io.winterframework.core.compiler.spi.ModuleInfoBuilder;
import io.winterframework.core.compiler.spi.SocketBeanInfo;

/**
 * <p>
 * Winter compiler annotation processor which processes {@link Module} and
 * {@link Bean} annotations and generate a Winter module class.
 * </p>
 * 
 * @author jkuhn
 *
 */
//@SupportedAnnotationTypes({"io.winterframework.core.annotation/io.winterframework.core.annotation.Module","io.winterframework.core.annotation/io.winterframework.core.annotation.Bean"})
@SupportedAnnotationTypes({"io.winterframework.core.annotation.Module","io.winterframework.core.annotation.Bean","io.winterframework.core.annotation.Configuration"})
@SupportedOptions({ModuleAnnotationProcessor.Options.DEBUG, ModuleAnnotationProcessor.Options.VERBOSE, ModuleAnnotationProcessor.Options.GENERATE_DESCRIPTOR})
public class ModuleAnnotationProcessor extends AbstractProcessor {

	public static final int VERSION = 1;
	
	private ModuleGenerator moduleGenerator;
	
	/**
	 * <p>
	 * Provides the options that can be passed to the module annotation processor.
	 * </p>
	 */
	public static class Options {
		
		public static final String DEBUG = "winter.debug";
		
		public static final String VERBOSE = "winter.verbose";
		
		public static final String GENERATE_DESCRIPTOR = "winter.generateDescriptor";
		
		private boolean generateModuleDescriptor;
		
		private boolean debug;
		
		private boolean verbose;
		
		public Options(Map<String, String> processingEnvOptions) {
			this.debug = processingEnvOptions.containsKey(DEBUG);
			this.verbose = processingEnvOptions.containsKey(VERBOSE);
			this.generateModuleDescriptor = processingEnvOptions.containsKey(GENERATE_DESCRIPTOR);
		}

		public boolean isDebug() {
			return debug;
		}

		public boolean isVerbose() {
			return verbose;
		}
		
		public boolean isGenerateModuleDescriptor() {
			return generateModuleDescriptor;
		}
	}
	
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return this.processingEnv.getSourceVersion().compareTo(SourceVersion.RELEASE_9) < 0 ? SourceVersion.RELEASE_9 : this.processingEnv.getSourceVersion();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if(this.moduleGenerator == null) {
			Options options = new ModuleAnnotationProcessor.Options(this.processingEnv.getOptions());
			
			TypeMirror moduleAnnotationType = this.processingEnv.getElementUtils().getTypeElement(Module.class.getCanonicalName()).asType();
			TypeMirror beanAnnotationType = this.processingEnv.getElementUtils().getTypeElement(Bean.class.getCanonicalName()).asType();
			TypeMirror configurationAnnotationType = this.processingEnv.getElementUtils().getTypeElement(Configuration.class.getCanonicalName()).asType();
		
			this.moduleGenerator = new ModuleGenerator(this.processingEnv, options);
			
			Map<String, List<Element>> moduleOriginatingElements = new HashMap<>();
		
			Map<String, ModuleInfoBuilder> moduleBuilders = new TreeMap<>(Collections.reverseOrder());
			Map<String, SocketBeanInfoFactory> socketFactories = new TreeMap<>(Collections.reverseOrder());
			Map<String, ConfigurationInfoFactory> configurationFactories = new TreeMap<>(Collections.reverseOrder());
			Map<String, ModuleBeanInfoFactory> beanFactories = new TreeMap<>(Collections.reverseOrder());
			
			roundEnv.getElementsAnnotatedWith(Module.class).stream()
				.forEach(element -> {
					String moduleName = ((ModuleElement)element).getQualifiedName().toString();
					ModuleInfoBuilder moduleInfoBuilder = ModuleInfoBuilderFactory.createModuleBuilder(this.processingEnv, (ModuleElement)element);
					moduleBuilders.put(moduleName, moduleInfoBuilder);
					if(!moduleOriginatingElements.containsKey(moduleName)) {
						moduleOriginatingElements.put(moduleName, new ArrayList<>());
					}
					moduleOriginatingElements.get(moduleName).add(element);
					
					beanFactories.put(moduleName, ModuleBeanInfoFactory.create(this.processingEnv, (ModuleElement)element));
					socketFactories.put(moduleName, SocketBeanInfoFactory.create(this.processingEnv, (ModuleElement)element));
					configurationFactories.put(moduleName, ConfigurationInfoFactory.create(this.processingEnv, (ModuleElement)element));
				});
			
			this.moduleGenerator.modules(moduleBuilders)
				.moduleConfigurations(roundEnv.getElementsAnnotatedWith(Configuration.class).stream() // Configurations should come first since they are required to create module beans
					.filter(element -> element.getKind().equals(ElementKind.INTERFACE))
					.filter(element -> element.getAnnotationMirrors().stream().anyMatch(a -> this.processingEnv.getTypeUtils().isSameType(a.getAnnotationType(), configurationAnnotationType)))
					.map(element -> {
						ConfigurationInfoFactory configurationFactory = null;
						for(Element moduleElement = element; moduleElement != null;moduleElement = moduleElement.getEnclosingElement()) {
							if(moduleElement instanceof ModuleElement) {
								configurationFactory = configurationFactories.get(((ModuleElement) moduleElement).getQualifiedName().toString());
								break;
							}
						}
						AnnotationMirror configurationAnnotation = element.getAnnotationMirrors().stream().filter(a -> this.processingEnv.getTypeUtils().isSameType(a.getAnnotationType(), configurationAnnotationType)).findFirst().get();
						if(configurationFactory == null) {
							this.processingEnv.getMessager().printMessage(Kind.MANDATORY_WARNING, "Configuration bean might be out of sync with the module please consider recompiling the module" , element, configurationAnnotation );
							return null;
						}
						ConfigurationInfo configuration;
						try {
							configuration = configurationFactory.createConfiguration(element);
						} 
						catch (BeanCompilationException | TypeErrorException e) {
							return null;
						}
						catch (Exception e) {
							this.processingEnv.getMessager().printMessage(Kind.WARNING, "Unable to create configuration bean: " + e.getMessage() , element, configurationAnnotation);
							return null;
						}
						moduleOriginatingElements.get(configuration.getQualifiedName().getModuleQName().getValue()).add(element);
						return configuration;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.groupingBy(configuration -> configuration.getQualifiedName().getModuleQName().getValue())))
				.moduleBeans(roundEnv.getElementsAnnotatedWith(Bean.class).stream()
					.filter(element -> element.getKind().equals(ElementKind.CLASS))
					.map(element -> {
						ModuleBeanInfoFactory beanFactory = null;
						for(Element moduleElement = element; moduleElement != null;moduleElement = moduleElement.getEnclosingElement()) {
							if(moduleElement instanceof ModuleElement) {
								beanFactory = beanFactories.get(((ModuleElement) moduleElement).getQualifiedName().toString());
								break;
							}
						}
						AnnotationMirror beanAnnotation = element.getAnnotationMirrors().stream().filter(a -> this.processingEnv.getTypeUtils().isSameType(a.getAnnotationType(), beanAnnotationType)).findFirst().get();
						if(beanFactory == null) {
							this.processingEnv.getMessager().printMessage(Kind.MANDATORY_WARNING, "Bean might be out of sync with the module please consider recompiling the module" , element, beanAnnotation );
							return null;
						}
						
						ModuleBeanInfo moduleBean;
						try {
							moduleBean = beanFactory.createBean(element);
						}
						catch (BeanCompilationException e) {
							return null;
						}
						catch (Exception e) {
							this.processingEnv.getMessager().printMessage(Kind.WARNING, "Unable to create bean: " + e.getMessage() , element, beanAnnotation );
							return null;
						}
						
						moduleOriginatingElements.get(moduleBean.getQualifiedName().getModuleQName().getValue()).add(element);
						return moduleBean;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.groupingBy(moduleBean -> moduleBean.getQualifiedName().getModuleQName().getValue())))
				.moduleSockets(roundEnv.getElementsAnnotatedWith(Bean.class).stream()
					.filter(element -> element.getKind().equals(ElementKind.INTERFACE))
					.filter(element -> element.getAnnotationMirrors().stream().noneMatch(a -> this.processingEnv.getTypeUtils().isSameType(a.getAnnotationType(), configurationAnnotationType)))
					.map(element -> {
						SocketBeanInfoFactory socketFactory = null;
						for(Element moduleElement = element; moduleElement != null;moduleElement = moduleElement.getEnclosingElement()) {
							if(moduleElement instanceof ModuleElement) {
								socketFactory = socketFactories.get(((ModuleElement) moduleElement).getQualifiedName().toString());
								break;
							}
						}
						AnnotationMirror beanAnnotation = element.getAnnotationMirrors().stream().filter(a -> this.processingEnv.getTypeUtils().isSameType(a.getAnnotationType(), beanAnnotationType)).findFirst().get();
						if(socketFactory == null) {
							this.processingEnv.getMessager().printMessage(Kind.MANDATORY_WARNING, "Module socket bean might be out of sync with the module please consider recompiling the module" , element, beanAnnotation );
							return null;
						}
						SocketBeanInfo moduleSocket;
						try {
							moduleSocket = socketFactory.createSocketBean(element);
						}
						catch (SocketCompilationException e) {
							return null;
						}
						catch (Exception e) {
							this.processingEnv.getMessager().printMessage(Kind.WARNING, "Unable to create socket bean: " + e.getMessage() , element, beanAnnotation );
							return null;
						}
						moduleOriginatingElements.get(moduleSocket.getQualifiedName().getModuleQName().getValue()).add(element);
						return moduleSocket;
					})
					.filter(Objects::nonNull)
					.collect(Collectors.groupingBy(socket -> socket.getQualifiedName().getModuleQName().getValue())))
				.componentModules(roundEnv.getElementsAnnotatedWith(Module.class).stream()
					.collect(Collectors.toMap(
						element -> ((ModuleElement)element).getQualifiedName().toString(), 
						element -> {
							ModuleElement moduleElement = (ModuleElement)element;

							AnnotationMirror moduleAnnotation = this.processingEnv.getElementUtils().getAllAnnotationMirrors(moduleElement).stream().filter(a -> this.processingEnv.getTypeUtils().isSameType(a.getAnnotationType(), moduleAnnotationType)).findFirst().get();
							
							final Set<String> includes = new HashSet<>();
							final Set<String> excludes = new HashSet<>();;
							for(Entry<? extends ExecutableElement, ? extends AnnotationValue> value : this.processingEnv.getElementUtils().getElementValuesWithDefaults(moduleAnnotation).entrySet()) {
								switch(value.getKey().getSimpleName().toString()) {
									case "includes" : includes.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
										break;
									case "excludes" : excludes.addAll(((List<AnnotationValue>)value.getValue().getValue()).stream().map(v -> v.getValue().toString()).collect(Collectors.toSet()));
										break;
								}
							}
							
							return moduleElement.getDirectives().stream()
								.filter(directive -> {
									if(!directive.getKind().equals(ModuleElement.DirectiveKind.REQUIRES)) {
										return false;
									}
									
									ModuleElement componentModuleElement = ((ModuleElement.RequiresDirective)directive).getDependency();

									if( (excludes.size() > 0 && excludes.contains(componentModuleElement.getQualifiedName().toString())) || (includes.size() > 0 && !includes.contains(componentModuleElement.getQualifiedName().toString()))) {
										return false;
									}
									
									Optional<? extends AnnotationMirror> componentModuleAnnotation = this.processingEnv.getElementUtils().getAllAnnotationMirrors(componentModuleElement).stream().filter(a -> this.processingEnv.getTypeUtils().isSameType(a.getAnnotationType(), moduleAnnotationType)).findFirst();
									return componentModuleAnnotation.isPresent();
								})
								.map(directive -> {
									ModuleElement componentModuleElement = ((ModuleElement.RequiresDirective)directive).getDependency();
									String componentModuleName = componentModuleElement.getQualifiedName().toString();
									if(moduleBuilders.containsKey(componentModuleName)) {
										return moduleBuilders.get(componentModuleName);
									}
								
									return this.processComponentModule(moduleElement, componentModuleElement);
								})
								.collect(Collectors.toList());
						}))
				)
				.originatingElements(moduleOriginatingElements);
		}
		this.moduleGenerator.generateNextRound();
		return true;
	}
	
	private ModuleInfoBuilder processComponentModule(ModuleElement moduleElement, ModuleElement componentModuleElement) {
		ModuleMetadataExtractor moduleMetadataExtractor = new ModuleMetadataExtractor(this.processingEnv, componentModuleElement);
		if(moduleMetadataExtractor.getModuleVersion() == null) {
			throw new IllegalStateException("Version of component module " + moduleMetadataExtractor.getModuleQualifiedName().toString() + " can't be null");			
		}
		TypeElement moduleType = this.processingEnv.getElementUtils().getTypeElement(moduleMetadataExtractor.getModuleQualifiedName().getClassName());
		
		switch(moduleMetadataExtractor.getModuleVersion()) {
			case 1: return this.processComponentModuleV1(moduleElement, componentModuleElement, moduleType);
			default: throw new IllegalStateException("Version of module " + moduleMetadataExtractor.getModuleQualifiedName().toString() + " is not supported: " + moduleMetadataExtractor.getModuleVersion());
		}
	}
	
	private ModuleInfoBuilder processComponentModuleV1(ModuleElement moduleElement, ModuleElement componentModuleElement, TypeElement moduleType) {
		ModuleInfoBuilder componentModuleBuilder = ModuleInfoBuilderFactory.createModuleBuilder(this.processingEnv, moduleElement, componentModuleElement, 1);
		
		SocketBeanInfoFactory componentModuleSocketFactory = SocketBeanInfoFactory.create(this.processingEnv, moduleElement, componentModuleElement, 1);
	
		List<? extends SocketBeanInfo> componentModuleSockets = ((ExecutableElement)moduleType.getEnclosedElements().stream()
			.filter(e -> e.getKind().equals(ElementKind.CONSTRUCTOR)).findFirst().get())
			.getParameters().stream()
			.map(ve -> {
				try {
					return componentModuleSocketFactory.createSocketBean(ve);
				} 
				catch (SocketCompilationException e1) {
					return null;
				}
			})
			.collect(Collectors.toList());

		componentModuleBuilder.sockets(componentModuleSockets.stream().toArray(SocketBeanInfo[]::new));

		ModuleBeanInfoFactory componentModuleBeanFactory = ModuleBeanInfoFactory.create(this.processingEnv, moduleElement, componentModuleElement, () -> componentModuleSockets, 1);
		List<? extends ModuleBeanInfo> componentModuleBeans = moduleType.getEnclosedElements().stream()
			.filter(e -> e.getKind().equals(ElementKind.METHOD) && e.getModifiers().contains(Modifier.PUBLIC) && !e.getModifiers().contains(Modifier.STATIC) && ((ExecutableElement)e).getParameters().size() == 0)
			.map(e -> {
				try {
					return componentModuleBeanFactory.createBean(e);
				} 
				catch (BeanCompilationException e1) {
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		componentModuleBuilder.beans(componentModuleBeans.stream().toArray(ModuleBeanInfo[]::new));
	
		return componentModuleBuilder;
	}
}
