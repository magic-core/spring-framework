/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.IOException;

/**
 * Convenient base class for {@link org.springframework.context.ApplicationContext}
 * implementations, drawing configuration from XML documents containing bean definitions
 * understood by an {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}.
 *
 * <p>Subclasses just have to implement the {@link #getConfigResources} and/or
 * the {@link #getConfigLocations} method. Furthermore, they might override
 * the {@link #getResourceByPath} hook to interpret relative paths in an
 * environment-specific fashion, and/or {@link #getResourcePatternResolver}
 * for extended pattern resolution.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConfigResources
 * @see #getConfigLocations
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader
 */
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableConfigApplicationContext {

	private boolean validating = true;


	/**
	 * Create a new AbstractXmlApplicationContext with no parent.
	 */
	public AbstractXmlApplicationContext() {
	}

	/**
	 * Create a new AbstractXmlApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	public AbstractXmlApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * Set whether to use XML validation. Default is {@code true}.
	 */
	public void setValidating(boolean validating) {
		this.validating = validating;
	}


	/**
	 * 解析 application.xml 文件中的 <bean/> 为 GenericBeanDefinition 实例，以 key 为 beanName，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
	 */
	@Override
	protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
		// 创建 XmlBeanDefinitionReader 实例,用于读取 application.xml 配置文件
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

		// resourceLoader 在 XmlBeanDefinitionReader 中负责读取 applicaiton.xml 文件为 Resource 实例
		// this 表示当前对象 ClassPathXmlApplicationContext 实例，继承自 ResourceLoader
		beanDefinitionReader.setResourceLoader(this);

		/** 非主要逻辑 */
		// ResourceEntityResolver，在后面的"非主要逻辑"里，将作为 Document 类中的一个成员变量，用于解析 Document 形式的application.xml文件
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		/** 无用逻辑-start */
		// getEnvironment 返回 StandardEnvironment 实例
		beanDefinitionReader.setEnvironment(this.getEnvironment());
		// 设置 beanDefinitionReader 验证xml (默认验证)
		initBeanDefinitionReader(beanDefinitionReader);
		/** 无用逻辑-end */

		// tofix 主线
		loadBeanDefinitions(beanDefinitionReader);
	}

	/**
	 * Initialize the bean definition reader used for loading the bean
	 * definitions of this context. Default implementation is empty.
	 * <p>Can be overridden in subclasses, e.g. for turning off XML validation
	 * or using a different XmlBeanDefinitionParser implementation.
	 * @param reader the bean definition reader used by this context
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setDocumentReaderClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
		reader.setValidating(this.validating);
	}

	/**
	 * 解析 application.xml 文件中的 <bean/> 为 GenericBeanDefinition 实例，以 key 为 beanName，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
	 *
	 * @param reader XmlBeanDefinitionReader 实例
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		/** Demo不涉及-start */
		// 调用 AbstractXmlApplicationContext 的 getConfigResources，返回空
		Resource[] configResources = getConfigResources();
		if (configResources != null) {
			reader.loadBeanDefinitions(configResources);
		}
		/** Demo不涉及-end */

		// 获取配置文件路径,例：返回["classpath*:applicationContext.xml"]
		// 调用 AbstractXmlApplicationContext 的 getConfigLocations 方法
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			// tofix 主线
			reader.loadBeanDefinitions(configLocations);
		}
	}

	/**
	 * Return an array of Resource objects, referring to the XML bean definition
	 * files that this context should be built with.
	 * <p>The default implementation returns {@code null}. Subclasses can override
	 * this to provide pre-built Resource objects rather than location Strings.
	 * @return an array of Resource objects, or {@code null} if none
	 * @see #getConfigLocations()
	 */
	@Nullable
	protected Resource[] getConfigResources() {
		return null;
	}

}
