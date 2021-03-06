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
package io.winterframework.core.v1;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>
 * A module wrapper which starts it as an application and stops it when the
 * application is gracefully shutdown. An application basically provides boiler
 * plate code to properly run a module as an application.
 * </p>
 * 
 * <p>
 * An application can display a {@link Banner} when it starts. By default, the
 * {@link StandardBanner} is displayed.
 * </p>
 * 
 * <p>
 * An application also registers a virtual-machine shutdown hook that gracefully
 * stops the module when the virtual machine shuts down.
 * </p>
 * 
 * @author jkuhn
 * @since 1.0
 * 
 * @param <T> the module type
 */
public class Application<T extends Module> {

	/**
	 * Application logger.
	 */
	private static Logger LOGGER = LogManager.getLogger(Application.class);

	/**
	 * The wrapped module builder.
	 */
	private Module.ModuleBuilder<T> moduleBuilder;

	/**
	 * The application banner.
	 */
	private Banner banner;

	/**
	 * The active module.
	 */
	private T module;

	/**
	 * <p>
	 * Creates a new Application for the module created with the specified builder.
	 * </p>
	 * 
	 * @param moduleBuilder the module builder.
	 */
	protected Application(Module.ModuleBuilder<T> moduleBuilder) {
		this.moduleBuilder = moduleBuilder;
		this.banner = new StandardBanner();
	}

	/**
	 * <p>
	 * Creates a new application for the module created with the specified builder.
	 * </p>
	 * 
	 * @param <E>           the module type.
	 * @param moduleBuilder the module builder.
	 * 
	 * @return an application.
	 */
	public static <E extends Module> Application<E> with(Module.ModuleBuilder<E> moduleBuilder) {
		return new Application<>(moduleBuilder);
	}

	/**
	 * <p>
	 * Creates and run a new application for the module created with the specified
	 * builder.
	 * </p>
	 * 
	 * @param <E>           the module type.
	 * @param moduleBuilder the module builder.
	 * 
	 * @return a running module instance.
	 */
	public static <E extends Module> E run(Module.ModuleBuilder<E> moduleBuilder) {
		return with(moduleBuilder).run();
	}

	/**
	 * <p>
	 * Sets the application banner.
	 * </p>
	 * 
	 * <p>
	 * If null is specified no banner will be displayed
	 * </p>
	 * 
	 * @param banner the banner to set or null to display no banner.
	 * 
	 * @return this application.
	 */
	public Application<T> banner(Banner banner) {
		this.banner = banner;
		return this;
	}

	/**
	 * <p>
	 * Runs the application.
	 * </p>
	 * 
	 * @return the resulting module.
	 * @throws IllegalStateException if the application is already running.
	 */
	public T run() throws IllegalStateException {
		if (this.module != null) {
			throw new IllegalStateException("Module " + this.module.getName() + " already started");
		}
		this.module = this.moduleBuilder.build();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			this.module.stop();
		}));

		if (this.banner != null) {
			LOGGER.info(() -> {
				ByteArrayOutputStream bannerStream = new ByteArrayOutputStream();
				this.banner.print(new PrintStream(bannerStream));
				return bannerStream.toString();
			});
		}
		this.module.start();

		return this.module;
	}
}
