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

package org.springframework.beans.factory.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Set;

/**
 * Abstract base class for bean definition readers which implement
 * the {@link BeanDefinitionReader} interface.
 *
 * <p>Provides common properties like the bean factory to work on
 * and the class loader to use for loading bean classes.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see BeanDefinitionReaderUtils
 * @since 11.12.2003
 */
public abstract class AbstractBeanDefinitionReader implements EnvironmentCapable, BeanDefinitionReader {

	/**
	 * Logger available to subclasses
	 */
	protected final Log logger = LogFactory.getLog(getClass());
	// DefaultListableBeanFactory实例,实例化XmlBeanDefinitionReader进行的赋值,XmlBeanDefinitionReader继承AbstractBeanDefinitionReader
	private final BeanDefinitionRegistry registry;

	@Nullable
	private ResourceLoader resourceLoader;

	@Nullable
	private ClassLoader beanClassLoader;

	private Environment environment;

	private BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();


	/**
	 * Create a new AbstractBeanDefinitionReader for the given bean factory.
	 * <p>If the passed-in bean factory does not only implement the BeanDefinitionRegistry
	 * interface but also the ResourceLoader interface, it will be used as default
	 * ResourceLoader as well. This will usually be the case for
	 * {@link org.springframework.context.ApplicationContext} implementations.
	 * <p>If given a plain BeanDefinitionRegistry, the default ResourceLoader will be a
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}.
	 * <p>If the passed-in bean factory also implements {@link EnvironmentCapable} its
	 * environment will be used by this reader.  Otherwise, the reader will initialize and
	 * use a {@link StandardEnvironment}. All ApplicationContext implementations are
	 * EnvironmentCapable, while normal BeanFactory implementations are not.
	 *
	 * @param registry the BeanFactory to load bean definitions into,
	 *                 in the form of a BeanDefinitionRegistry
	 * @see #setResourceLoader
	 * @see #setEnvironment
	 */
	protected AbstractBeanDefinitionReader(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;

		// Determine ResourceLoader to use.
		if (this.registry instanceof ResourceLoader) {
			this.resourceLoader = (ResourceLoader) this.registry;
		} else {
			this.resourceLoader = new PathMatchingResourcePatternResolver();
		}

		// Inherit Environment if possible
		if (this.registry instanceof EnvironmentCapable) {
			this.environment = ((EnvironmentCapable) this.registry).getEnvironment();
		} else {
			this.environment = new StandardEnvironment();
		}
	}


	public final BeanDefinitionRegistry getBeanFactory() {
		return this.registry;
	}

	/**
	 * DefaultListableBeanFactory实例,实例化XmlBeanDefinitionReader进行的赋值,XmlBeanDefinitionReader继承AbstractBeanDefinitionReader
	 *
	 * @return
	 */
	@Override
	public final BeanDefinitionRegistry getRegistry() {
		// DefaultListableBeanFactory实例,实例化XmlBeanDefinitionReader进行的赋值,XmlBeanDefinitionReader继承AbstractBeanDefinitionReader
		return this.registry;
	}

	/**
	 * Set the ResourceLoader to use for resource locations.
	 * If specifying a ResourcePatternResolver, the bean definition reader
	 * will be capable of resolving resource patterns to Resource arrays.
	 * <p>Default is PathMatchingResourcePatternResolver, also capable of
	 * resource pattern resolving through the ResourcePatternResolver interface.
	 * <p>Setting this to {@code null} suggests that absolute resource loading
	 * is not available for this bean definition reader.
	 *
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	@Nullable
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * Set the ClassLoader to use for bean classes.
	 * <p>Default is {@code null}, which suggests to not load bean classes
	 * eagerly but rather to just register bean definitions with class names,
	 * with the corresponding Classes to be resolved later (or never).
	 *
	 * @see Thread#getContextClassLoader()
	 */
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Override
	@Nullable
	public ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}

	/**
	 * Set the Environment to use when reading bean definitions. Most often used
	 * for evaluating profile information to determine which bean definitions
	 * should be read and which should be omitted.
	 */
	public void setEnvironment(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		this.environment = environment;
	}

	@Override
	public Environment getEnvironment() {
		return this.environment;
	}

	/**
	 * Set the BeanNameGenerator to use for anonymous beans
	 * (without explicit bean name specified).
	 * <p>Default is a {@link DefaultBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(@Nullable BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : new DefaultBeanNameGenerator());
	}

	@Override
	public BeanNameGenerator getBeanNameGenerator() {
		return this.beanNameGenerator;
	}


	/**
	 * 遍历每个 Resource （本 Demo 只有一个），解析 Resource 中的 <bean/> 为 GenericBeanDefinition 实例，以 beanName 为 key，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
	 *
	 * @param resources the resource descriptors
	 * @return
	 * @throws BeanDefinitionStoreException
	 */
	@Override
	public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
		Assert.notNull(resources, "Resource array must not be null");
		// 表示加载到的有效 <bean/> 个数
		int counter = 0;
		// 遍历 resources（xml 文件资源）
		for (Resource resource : resources) {
			// 调用从 XmlBeanDefinitionReader 实现的 loadBeanDefinitions 方法
			counter += loadBeanDefinitions(resource);
		}
		return counter;
	}

	/**
	 * 根据 locations 获取 application.xml 文件，解析 application.xml 文件中的 <bean/> 为 GenericBeanDefinition 实例，以 beanName 为 key，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
	 *
	 * @param location 配置文件路径,例："classpath*:applicationContext.xml"
	 * @return
	 * @throws BeanDefinitionStoreException
	 */
	@Override
	public int loadBeanDefinitions(String location) throws BeanDefinitionStoreException {
		return loadBeanDefinitions(location, null);
	}

	/**
	 * 解析 application.xml 文件中的 <bean/> 为 GenericBeanDefinition 实例，以 beanName 为 key，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
	 *
	 * @param location        xml 配置资源路径
	 * @param actualResources 作用：根据 actualResources 中是否存在重复资源而判断是否循环 <import/> application.xml 配置文件
	 *                        使用场景：
	 *                        如果正常解析 xml 配置文件，actualResources 为空
	 *                        如果解析 xml 配置文件时遇到 <import/>，则会递归，再次调用本方法去解析 <import/>中指定的 xml 文件资源，actualResources 会传递一个 set 实例，虽不为空但不包含元素
	 * @return 加载到的 GenericBeanDefinition 数量
	 * @throws BeanDefinitionStoreException in case of loading or parsing errors
	 */
	public int loadBeanDefinitions(String location, @Nullable Set<Resource> actualResources) throws BeanDefinitionStoreException {
		// resourceLoader 表示 ClassPathXmlApplicationContext 实例，是 XmlBeanDefinitionReader 中负责读取 applicaiton.xml 文件为 Resource 实例
		ResourceLoader resourceLoader = getResourceLoader();
		if (resourceLoader == null) {
			throw new BeanDefinitionStoreException(
					"Cannot import bean definitions from location [" + location + "]: no ResourceLoader available");
		}

		// ClassPathXmlApplicationContext 继承 ResourcePatternResolver，所以走本分支
		if (resourceLoader instanceof ResourcePatternResolver) {
			try {
				// 获得配置文件的资源实例，一个 Resource 实例代表一个 application.xml 文件
				// getResources 方法调用的是从继承 AbstractApplicationContext 来的
				Resource[] resources = ((ResourcePatternResolver) resourceLoader).getResources(location);

				// tofix 主线
				// 解析 Resource 中的 <bean/> 为 GenericBeanDefinition 实例，以 beanName 为 key，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
				int loadCount = loadBeanDefinitions(resources);

				// 作用：根据 actualResources 中是否存在重复资源来判断是否循环导入 xml 配置文件了
				// 使用场景：和 <import/> 相关;如果正常解析 xml 配置文件，actualResources 为空,如果解析 xml 配置文件时,遇到 import 标签，则会递归，再次调用本方法去解析 import 中指定的 xml 文件资源，actualResources 会传递一个 set 实例，虽不为空但不包含元素
				if (actualResources != null) {
					for (Resource resource : resources) {
						actualResources.add(resource);
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Loaded " + loadCount + " bean definitions from location pattern [" + location + "]");
				}
				return loadCount;
			} catch (IOException ex) {
				throw new BeanDefinitionStoreException(
						"Could not resolve bean definition resource pattern [" + location + "]", ex);
			}
		}
		/** Demo不涉及 */
		else {
			Resource resource = resourceLoader.getResource(location);
			int loadCount = loadBeanDefinitions(resource);
			if (actualResources != null) {
				actualResources.add(resource);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + loadCount + " bean definitions from location [" + location + "]");
			}
			return loadCount;
		}
	}

	/**
	 * 根据 locations 获取 application.xml 文件，解析 application.xml 文件中的 <bean/> 为 GenericBeanDefinition 实例，以 beanName 为 key，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
	 *
	 * @param locations 配置文件路径,例：返回["classpath*:applicationContext.xml"]
	 * @return
	 * @throws BeanDefinitionStoreException
	 */
	@Override
	public int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException {
		Assert.notNull(locations, "Location array must not be null");
		// counter 表示当前操作加载 <bean/> 的数量
		int counter = 0;
		// 遍历用户指定的 application.xml 配置路径（比如"classpath*:applicationContext.xml"），解析 xml 中的 <bean/> 到 DefaultListableBeanFactory 工厂里
		for (String location : locations) {
			// tofix 主线
			counter += loadBeanDefinitions(location);
		}
		return counter;
	}

}
