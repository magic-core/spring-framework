/*
 * Copyright 2002-2018 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	/**
	 * 执行场景：bean定义组装好实体后，实例化前
	 * 作用：Spring将bean工厂对象作为参数，调用所有实现了 BeanDefinitionRegistryPostProcessor、BeanFactoryPostProcessor 的bean的重写方法，借此让这些bean自定义处理逻辑
	 * 大体逻辑：
	 * 1.查找所有实现了 BeanDefinitionRegistryPostProcessor 的bean，遍历执行重写方法,将bean工厂对象作为参数
	 * 2.查找所有实现了 BeanFactoryPostProcessor 的bean，遍历执行重写方法,将bean工厂对象作为参数
	 *
	 * @param beanFactory
	 * @param beanFactoryPostProcessors
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {
		Set<String> processedBeans = new HashSet<>();

		// 走本分支
		// beanFactory 是 DefaultListableBeanFactory 实例，继承BeanDefinitionRegistry
		if (beanFactory instanceof BeanDefinitionRegistry) {
			// registry 代表bean工厂
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			// 没有执行场景，不讲解
			// 通过查看调用关系，当前Spring实现来看，beanFactoryPostProcessors一直为空集合。
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				} else {
					regularPostProcessors.add(postProcessor);
				}
			}
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);

			/**操作实现了 BeanDefinitionRegistryPostProcessor 的bean*/

			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// postProcessorNames 表示实现了 BeanDefinitionRegistryPostProcessor 的所有beanName集合；
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);

			// 当前Demo没有执行本逻辑,不讲解
			// 1.只有定义的Bean实现了 BeanDefinitionRegistryPostProcessor 和 PriorityOrdered ，本逻辑才有效，该bean可以在进行实例化bean步骤前，得到bean工厂实例
			// 循环遍历 postProcessorNames
			for (String ppName : postProcessorNames) {
				// 如果beanName是ppName的bean，同样实现了PriorityOrdered
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					// 添加到集合里
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// 使用者一般不使用，在Spring内部，也没有执行本逻辑的场景，不讲解
			// 2.只有定义的Bean实现了 BeanDefinitionRegistryPostProcessor 和 Ordered,并且没有实现PriorityOrdered ，本逻辑才有效，该bean可以在所有的bean实例化前，得到bean工厂实例
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// 使用者一般不使用，在Spring内部，也没有执行本逻辑的场景，不讲解
			// 3.只有定义的Bean实现了 BeanDefinitionRegistryPostProcessor 并且没有实现PriorityOrdered和Ordered ，本逻辑才有效，该bean可以在所有的bean实例化前，得到bean工厂实例
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// 循环调用所有实现了 BeanDefinitionRegistryPostProcessor bean的postProcessBeanFactory方法
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
		}
		// 如果beanFactory 不是BeanDefinitionRegistry的子类
		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		/**操作实现了 BeanFactoryPostProcessor 的bean*/

		// priorityOrderedPostProcessors 用于装载实现了 BeanFactoryPostProcessor ,同时实现了 PriorityOrdered的bean
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		// orderedPostProcessorNames 用于装载实现了 BeanFactoryPostProcessor ,同时实现了 Ordered 的bean的beanName
		List<String> orderedPostProcessorNames = new ArrayList<>();
		// nonOrderedPostProcessorNames 用于装载实现了 BeanFactoryPostProcessor ,但是没有实现任何用于排序的接口的bean的beanName
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		// 获得bean工厂中实现了BeanFactoryPostProcessor的bean定义集合 postProcessorNames
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);
		// 遍历 postProcessorNames 集合
		for (String ppName : postProcessorNames) {
			// 如果postProcessorNames包含当前的ppName（beanName）；
			// 则说明上面的逻辑已经处理过了这个beanName代表的Bean定义；
			// 因为BeanDefinitionRegistryPostProcessor 也实现了 BeanFactoryPostProcessor，所以可能出现这种情况
			if (processedBeans.contains(ppName)) {
				// 什么都不做
			}
			// 如果ppName(beanName)指代的bean定义对象，还实现了PriorityOrdered
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			// 如果ppName(beanName)指代的bean定义对象，还实现了Ordered
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			// 如果ppName(beanName)指代的bean定义对象，没有实现任何排序相关的接口
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}
		// 对priorityOrderedPostProcessors集合进行排序，默认按照OrderComparator的实现进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 循环遍历 priorityOrderedPostProcessors集合，执行每个实例的postProcessBeanFactory方法，参数为beanFactory
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// 当前Demo没有执行本逻辑,不讲解
		// 循环遍历orderedPostProcessorNames，执行每个 BeanFactoryPostProcessor 实例的postProcessBeanFactory方法
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// 当前Demo没有执行本逻辑,不讲解
		// 循环遍历nonOrderedPostProcessorNames，执行每个 BeanFactoryPostProcessor 实例的postProcessBeanFactory方法
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	/**
	 * 向bean工厂注册实现 BeanPostProcessor 接口的bean（后置处理器）
	 *
	 * @param beanFactory
	 * @param applicationContext
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		// 获得实现了 BeanPostProcessor 的bean的beanName集合
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		/**将实现 BeanPostProcessor 的 BeanPostProcessorChecker 注册到bean工厂的后置处理器列表里*/
		// 计算后置处理器的总数（已经注册到bean工厂的后置处理器数量+没有注册到的）
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));


		/**根据实现种类，组装 BeanPostProcessor 集合数据*/
		for (String ppName : postProcessorNames) {
			// 如果当前ppName所指的bean，还实现了PriorityOrdered
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				// 获得当前ppName代表的bean，添加到 priorityOrderedPostProcessors 集合里
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				// 如果pp 属于 MergedBeanDefinitionPostProcessor，添加到 internalPostProcessors 集合里
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}

				// 如果当前ppName所指的bean还实现了 Ordered，添加到 orderedPostProcessorNames 集合里
			} else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);

				// 如果当前ppName所指的bean，没有实现任何排序相关的接口，添加到 nonOrderedPostProcessorNames 集合里
			} else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		/**将实现BeanPostProcessor，还实现了PriorityOrdered的bean集合排序后，注册到bean工厂的后置处理器列表里*/
		// 对 priorityOrderedPostProcessors 集合进行排序，默认按照OrderComparator类的实现进行排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		// 向 beanFactory 添加 priorityOrderedPostProcessors 后置处理器
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		/**将实现 BeanPostProcessor，还实现了 Ordered 的bean集合排序后，注册到bean工厂的后置处理器列表里*/
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			// 如果pp 属于 MergedBeanDefinitionPostProcessor，添加到 internalPostProcessors 集合里
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		/**将只实现 BeanPostProcessor，没有实现任何排序相关的接口的bean集合排序后，注册到bean工厂的后置处理器列表里*/
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			// 如果pp 属于 MergedBeanDefinitionPostProcessor，添加到 internalPostProcessors 集合里
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		/**将只实现 BeanPostProcessor，没有实现任何排序相关的接口的bean集合注册到bean工厂的后置处理器列表里*/
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		/**最后初始化ApplicationListenerDetector，作为后置处理器，注册到bean工厂的后置处理器列表的末端*/
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
