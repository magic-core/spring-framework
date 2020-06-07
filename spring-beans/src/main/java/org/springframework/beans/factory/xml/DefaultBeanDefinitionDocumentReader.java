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

package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Default implementation of the {@link BeanDefinitionDocumentReader} interface that
 * reads bean definitions according to the "spring-beans" DTD and XSD format
 * (Spring's default XML bean definition format).
 *
 * <p>The structure, elements, and attribute names of the required XML document
 * are hard-coded in this class. (Of course a transform could be run if necessary
 * to produce this format). {@code <beans>} does not need to be the root
 * element of the XML document: this class will parse all bean definition elements
 * in the XML file, regardless of the actual root element.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Erik Wiersma
 * @since 18.12.2003
 */
public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

	public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

	public static final String NESTED_BEANS_ELEMENT = "beans";

	public static final String ALIAS_ELEMENT = "alias";

	public static final String NAME_ATTRIBUTE = "name";

	public static final String ALIAS_ATTRIBUTE = "alias";

	public static final String IMPORT_ELEMENT = "import";

	public static final String RESOURCE_ATTRIBUTE = "resource";

	public static final String PROFILE_ATTRIBUTE = "profile";


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private XmlReaderContext readerContext;

	@Nullable
	private BeanDefinitionParserDelegate delegate;


	/**
	 * 解析 Document 中的 <bean/> 为 GenericBeanDefinition 实例，以 beanName 为 key，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
	 *
	 * @param doc 代表xml配置文件的DOM document实例
	 * @param readerContext 用于将XmlBeanDefinitionReader实例、xml资源实例等共用实例统一封装到一个对象里，向后传递使用
	 * (includes the target registry and the resource being parsed)
	 */
	@Override
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		this.readerContext = readerContext;
		logger.debug("Loading bean definitions");

		// 获得代表xml配置文件的Document实例根节点root,即<beans/>节点
		Element root = doc.getDocumentElement();
		// tofix 主线
		// 根据xml根节点，即<beans/>节点，加载xml中所有的bean定义
		doRegisterBeanDefinitions(root);
	}

	/**
	 * Return the descriptor for the XML resource that this parser works on.
	 */
	protected final XmlReaderContext getReaderContext() {
		Assert.state(this.readerContext != null, "No XmlReaderContext available");
		return this.readerContext;
	}

	/**
	 * Invoke the {@link org.springframework.beans.factory.parsing.SourceExtractor}
	 * to pull the source metadata from the supplied {@link Element}.
	 */
	@Nullable
	protected Object extractSource(Element ele) {
		return getReaderContext().extractSource(ele);
	}


	/**
	 * 根据xml文件的root节点，解析 Document 中的 <bean/> 为 GenericBeanDefinition 实例，以 beanName 为 key，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
	 *
	 * @param root
	 */
	protected void doRegisterBeanDefinitions(Element root) {

		/** 空变量 */
		// 表示当前<beans/>的父<beans/>中的bean定义解析委托类 BeanDefinitionParserDelegate 实例；
		BeanDefinitionParserDelegate parent = this.delegate;

		// 创建 BeanDefinitionParserDelegate 实例(bean定义的解析委托类),定义了解析XML文件（Doc形式）的一系列方法，是核心解析器
		// getReaderContext() 返回 XmlReaderContext 实例
		this.delegate = createDelegate(getReaderContext(), root, parent);

		/** Demo不涉及 */
		// 如果node隶属默认命名空间”http://www.springframework.org/schema/beans“（即非用户自定义的node节点）
		if (this.delegate.isDefaultNamespace(root)) {
			// profileSpec表示root节点(即<beans/>)的"profile"属性,可以根据指定的环境变量，动态切换<beans/>
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			if (StringUtils.hasText(profileSpec)) {
				String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
						profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
				if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
					if (logger.isInfoEnabled()) {
						logger.info("Skipped XML bean definition file due to specified profiles [" + profileSpec +
								"] not matching: " + getReaderContext().getResource());
					}
					return;
				}
			}
		}

		// 模版模式,默认空实现,用于子类继承，自定义逻辑
		preProcessXml(root);/** 空实现 */

		// tofix 主线
		// 根据xml文件的root节点，即<beans/>节点，加载xml中所有的bean定义
		parseBeanDefinitions(root, this.delegate);

		// 模版模式,默认空实现,用于子类继承，自定义逻辑
		postProcessXml(root);/** 空实现 */

		/** 空变量 */
		this.delegate = parent;
	}

	/**
	 * 创建 BeanDefinitionParserDelegate 实例，具体负责将XML文件（Document形式）中的bean定义装载为Spring内置的实体类`GenericBeanDefinition`
	 *
	 * @param readerContext
	 * @param root
	 * @param parentDelegate 父XML代表的bean定义委托类。
	 * @return
	 */
	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {
		// 初始化 BeanDefinitionParserDelegate 实例，在bean工厂中，具体负责将XML文件（Document形式）中的bean定义装载为Spring内置的实体类`GenericBeanDefinition`
		// readerContext 表示 XmlReaderContext  实例
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		// 根据beans中的配置，配置 BeanDefinitionParserDelegate 的相应属性
		delegate.initDefaults(root, parentDelegate);
		return delegate;
	}

	/**
	 * 根据xml文件的root节点，即<beans/>节点，解析 Document 中的 <bean/> 为 GenericBeanDefinition 实例，以 beanName 为 key，放到 Map<String, BeanDefinition> beanDefinitionMap 里保存
	 *
	 * @param root xml 的根节点，即<beans/>
	 * @param delegate BeanDefinitionParserDelegate 实例(bean定义的解析委托类),定义了解析XML文件（Doc形式）的一系列方法，是核心解析器
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		// 如果 root根节点 隶属默认命名空间,默认命名空间:"http://www.springframework.org/schema/beans",走本分支
		if (delegate.isDefaultNamespace(root)) {
			// nl 表示 根节点 的子节点，即<beans/>下的所有节点，不包括beans本身
			NodeList nl = root.getChildNodes();
			// 遍历根节点的子节点
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				// 如果子节点属于元素节点，走本分支,例：<bean/>就是元素节点；换行符、注释就不是节点
				if (node instanceof Element) {
					Element ele = (Element) node;
					// 如果当前元素是不存在命名空间或者属于"http://www.springframework.org/schema/beans"命名空间，走本分支；例：<bean/>、<import/>等
					if (delegate.isDefaultNamespace(ele)) {
						// tofix 主线
						// 解析当前的ele节点，根据节点类型，做不同操作；如果是<bean/>,注册到bean工厂里；如果是<import/>,则将resource，递归重新调用AbstractBeanDefinitionReader#loadBeanDefinitions方法，解析指定的xml资源路径
						parseDefaultElement(ele, delegate);
					}
					/** Demo不涉及 */
					// 如果当前元素不隶属默认命名空间，例：Spring中其它命名空间的标签（<context:property-placeholder location="classpath*:application.properties" />）、用户自定义标签
					else {
						// 解析当前的ele节点，根据节点的命名空间，找到相应的处理器进行处理
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		/** Demo不涉及 */
		// 如果当前根节点root不隶属默认命名空间,例：Spring中其它命名空间的标签（<context:property-placeholder location="classpath*:application.properties" />）、用户自定义标签
		else {
			delegate.parseCustomElement(root);
		}
	}

	/**
	 * 解析当前的ele节点，根据节点类型，加载到bean工厂里；
	 * 如果是<bean/>,注册到bean工厂里;
	 * 如果是<import/>,则根据配置项resource，递归重新调用AbstractBeanDefinitionReader#loadBeanDefinitions方法，解析指定路径的xml资源，加载到bean工厂里
	 *
	 * @param ele 当前节点，可能是'import'、'alias'、'bean'、'beans'
	 * @param delegate BeanDefinitionParserDelegate 实例(bean定义的解析委托类),定义了解析XML文件（Doc形式）的一系列方法，是核心解析器
	 */
	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		// 如果节点名是'import'
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			// 如果是<import/>,则根据配置项resource，递归重新调用AbstractBeanDefinitionReader#loadBeanDefinitions方法，解析指定路径的xml资源
			importBeanDefinitionResource(ele);
		}
		// 如果节点名是'alias'
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
		}
		// tofix 主线
		// 如果节点名是'bean'
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			// 如果是<bean/>,注册到bean工厂里;
			processBeanDefinition(ele, delegate);
		}
		// 如果节点名是'beans'
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// recurse
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * 如果是<import/>,则根据配置项'resource'，递归重新调用AbstractBeanDefinitionReader#loadBeanDefinitions方法，解析指定的xml资源路径，加载到bean工厂里
	 *
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 * @param ele 当前标签代表的元素
	 */
	protected void importBeanDefinitionResource(Element ele) {
		// 获得<import/>标签指定的'resource'属性
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		// 如果resource属性不包含值,则报错
		if (!StringUtils.hasText(location)) {
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// 如果import导入的xml文件路径包含占位符,则替换占位符 例如: "${user.dir}"
		// getReaderContext()返回 XmlReaderContext 实例，通过 XmlReaderContext 获取所需的公用实例
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<>(4);

		// absoluteLocation 表示配置的xml文件路径是否是绝对路径,例:classpath*:是绝对路径
		boolean absoluteLocation = false;
		try {
			// 判断配置的xml文件路径是绝对路径还是相对路径
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		} catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// 如果是绝对路径,例:classpath*；一般使用本路径模式，走本分支
		if (absoluteLocation) {
			try {
				// 递归调用 loadBeanDefinitions 方法,加载import导入进来的xml文件中的配置
				// getReaderContext()返回 XmlReaderContext 实例，通过 XmlReaderContext 获取所需的公用实例
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		// 如果是相对路径；Demo不涉及，暂不深解
		else {
			// No URL -> considering resource location as relative to the current file.
			try {
				int importCount;
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				} else {
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			} catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]",
						ele, ex);
			}
		}
		Resource[] actResArray = actualResources.toArray(new Resource[0]);
		getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
	}

	/**
	 * Process the given alias element, registering the alias with the registry.
	 */
	protected void processAliasRegistration(Element ele) {
		String name = ele.getAttribute(NAME_ATTRIBUTE);
		String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
		boolean valid = true;
		if (!StringUtils.hasText(name)) {
			getReaderContext().error("Name must not be empty", ele);
			valid = false;
		}
		if (!StringUtils.hasText(alias)) {
			getReaderContext().error("Alias must not be empty", ele);
			valid = false;
		}
		if (valid) {
			try {
				getReaderContext().getRegistry().registerAlias(name, alias);
			} catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * 如果是<bean/>,解析配置并注册到bean工厂里;
	 *
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 * @param ele 当前节点
	 * @param delegate BeanDefinitionParserDelegate 实例(bean定义的解析委托类),定义了解析XML文件（Doc形式）的一系列方法，是核心解析器
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		// tofix 主线
		// 解析ele所代表的bean节点信息,创建BeanDefinitionHolder实例
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			/** Demo不涉及 */
			// 这个方法是用于解析xml文件中<bean/>里使用者自己开发的自定义标签，使用自定义标签需要另编写XSD文件、命名空间解析器等；
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);

			try {
				// tofix 主线
				// 将最终解析成的bean定义,存储到(注册)bean工厂里
				// getReaderContext()返回 XmlReaderContext 实例，通过 XmlReaderContext 获取所需的公用实例
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());

			} catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			/** 空实现 */
			// 用于将bean注册到bean工厂的事件通知给监听者，Spring只写了通知的相关逻辑，但实际上监听者没有监听逻辑
			getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
		}
	}


	/**
	 * Allow the XML to be extensible by processing any custom element types first,
	 * before we start to process the bean definitions. This method is a natural
	 * extension point for any other custom pre-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void preProcessXml(Element root) {
	}

	/**
	 * Allow the XML to be extensible by processing any custom element types last,
	 * after we finished processing the bean definitions. This method is a natural
	 * extension point for any other custom post-processing of the XML.
	 * <p>The default implementation is empty. Subclasses can override this method to
	 * convert custom elements into standard Spring bean definitions, for example.
	 * Implementors have access to the parser's bean definition reader and the
	 * underlying XML resource, through the corresponding accessors.
	 *
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
