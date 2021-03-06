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
package io.winterframework.core.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.winterframework.test.WinterCompilationException;
import io.winterframework.test.WinterTestCompiler;
import io.winterframework.test.WinterModuleLoader;
import io.winterframework.test.WinterModuleProxy;

/**
 * 
 * @author jkuhn
 *
 */
public class TestMultiModule extends AbstractCoreWinterTest {

	private static final String MODULEA = "io.winterframework.test.multi.moduleA";
	private static final String MODULEB = "io.winterframework.test.multi.moduleB";
	private static final String MODULEC = "io.winterframework.test.multi.moduleC";
	private static final String MODULED = "io.winterframework.test.multi.moduleD";
	private static final String MODULEE = "io.winterframework.test.multi.moduleE";
	private static final String MODULEF = "io.winterframework.test.multi.moduleF";
	private static final String MODULEG = "io.winterframework.test.multi.moduleG";
	private static final String MODULEH = "io.winterframework.test.multi.moduleH";

	@Test
	public void testMultiModuleSimple() throws IOException, WinterCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		WinterModuleLoader moduleLoader = this.getWinterCompiler().compile(MODULEA, MODULEB);
		
		WinterModuleProxy moduleA = moduleLoader.load(MODULEA).build();
		moduleA.start();
		try {
			Object beanA = moduleA.getBean("beanA");
			Assertions.assertNotNull(beanA);
		}
		finally {
			moduleA.stop();
		}
		
		WinterModuleProxy moduleB = moduleLoader.load(MODULEB).build();
		moduleB.start();
		try {
			Object beanB = moduleB.getBean("beanB");
			Assertions.assertNotNull(beanB);
			
			Object beanB_beanA = beanB.getClass().getField("beanA").get(beanB);
			Assertions.assertNotNull(beanB_beanA);
		}
		finally {
			moduleB.stop();
		}
	}
	
	@Test
	public void testMultiModuleImport() throws IOException, WinterCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		this.getWinterCompiler().compile(MODULEA, MODULEB);
		
		WinterTestCompiler extraCompiler = this.getWinterCompiler().withModulePaths(List.of(new File(this.getWinterCompiler().getModuleOutputPath(), MODULEA), new File(this.getWinterCompiler().getModuleOutputPath(), MODULEB)));
		WinterModuleLoader moduleLoader = extraCompiler.compile(MODULEC);
		WinterModuleProxy moduleC = moduleLoader.load(MODULEC).build();
		moduleC.start();
		try {
			Object beanC = moduleC.getBean("beanC");
			Assertions.assertNotNull(beanC);
			
			Object beanC_beanA = beanC.getClass().getField("beanA").get(beanC);
			Assertions.assertNotNull(beanC_beanA);
			Object beanC_beanB = beanC.getClass().getField("beanB").get(beanC);
			Assertions.assertNotNull(beanC_beanB);
			
			Object beanC_beanB_beanA = beanC_beanB.getClass().getField("beanA").get(beanC_beanB);
			Assertions.assertNotNull(beanC_beanB_beanA);
			Assertions.assertNotEquals(beanC_beanA, beanC_beanB_beanA);
		}
		finally {
			moduleC.stop();
		}
	}
	
	@Test
	public void testMultiModuleSocket() throws IOException, WinterCompilationException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		WinterModuleLoader moduleLoader = this.getWinterCompiler().compile(MODULEA, MODULED, MODULEE, MODULEF);
		
		WinterModuleProxy moduleD = moduleLoader.load(MODULED).build();
		moduleD.start();
		try {
			Object beanD = moduleD.getBean("beanD");
			Assertions.assertNotNull(beanD);
			
			Object beanD_beanE = beanD.getClass().getField("beanE").get(beanD);
			Assertions.assertNotNull(beanD_beanE);
			
			Object beanD_beanE_beanA = beanD_beanE.getClass().getField("beanA").get(beanD_beanE);
			Assertions.assertNotNull(beanD_beanE_beanA);
			
			Object beanD_beanF = beanD.getClass().getField("beanF").get(beanD);
			Assertions.assertNotNull(beanD_beanF);
			
			Object beanD_beanF_beanA = beanD_beanE.getClass().getField("beanA").get(beanD_beanE);
			Assertions.assertNotNull(beanD_beanF_beanA);
		}
		finally {
			moduleD.stop();
		}
	}
	
	@Test
	public void testComponentModuleBeanExclusionWiring() throws IOException, WinterCompilationException {
		WinterModuleLoader moduleLoader = this.getWinterCompiler().compile(MODULEG, MODULEH);
		moduleLoader.load(MODULEH).build();
	}
}