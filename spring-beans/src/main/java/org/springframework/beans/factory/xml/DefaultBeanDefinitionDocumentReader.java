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
	 * This implementation parses bean definitions according to the "spring-beans" XSD
	 * (or DTD, historically).
	 * <p>Opens a DOM Document; then initializes the default settings
	 * specified at the {@code <beans/>} level; then parses the contained bean definitions.
	 */
	@Override
	public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
		// readerContext 代表 XmlReaderContext 实例，tofix 用于？
		// DefaultBeanDefinitionDocumentReader：setreaderContext
		this.readerContext = readerContext;
		logger.debug("Loading bean definitions");

		// 获得代表xml配置文件的Document实例根节点root,即<beans/>节点
		Element root = doc.getDocumentElement();
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
	 * 根据xml文件的root节点，即<beans/>节点，加载xml中所有的bean定义
	 */
	protected void doRegisterBeanDefinitions(Element root) {
		// delegate成员变量，代表 BeanDefinitionParserDelegate 实例(bean定义的解析委托类),定义了解析XML文件的一系列方法，是核心解析器
		// parent 局部变量，表示父<beans/>中的bean定义解析委托类 BeanDefinitionParserDelegate 实例
		// 如果当前流程下，this.delegate不为空，代表发生了递归，即xml文件还引入了一个</beans>标签
		BeanDefinitionParserDelegate parent = this.delegate;
		// 对delegate进行赋值
		// getReaderContext() 返回 XmlReaderContext 实例
		// DefaultBeanDefinitionDocumentReader：setdelegate
		this.delegate = createDelegate(getReaderContext(), root, parent);
		// 如果node隶属默认命名空间”http://www.springframework.org/schema/beans“
		if (this.delegate.isDefaultNamespace(root)) {
			// 获得root节点(即<beans/>)的"profile"属性,根据指定的环境变量，动态切换<beans/>，没有企业使用这种切换环境变量的方式，都是使用maven/gradle等方式来切换
			String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
			// 如果<beans/>配置了profile属性；几乎不被使用，粉丝无需理会
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
		preProcessXml(root);
		// 根据root遍历子节点，注册bean定义
		parseBeanDefinitions(root, this.delegate);
		// 模版模式,默认空实现,用于子类继承，自定义逻辑
		postProcessXml(root);

		this.delegate = parent;
	}

	/**
	 *
	 * 创建 BeanDefinitionParserDelegate 实例，用于解析XML bean定义的有状态（有成员变量）委托类。
	 *
	 * @param readerContext
	 * @param root
	 * @param parentDelegate 父XML代表的bean定义委托类。
	 * @return
	 */
	protected BeanDefinitionParserDelegate createDelegate(
			XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {
		// 初始化 BeanDefinitionParserDelegate 实例，用于解析XML bean定义的有状态（有成员变量）委托类。
		// readerContext 表示 XmlReaderContext  实例
		// BeanDefinitionParserDelegate；
		BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
		// 根据beans中的配置，初始化BeanDefinitionParserDelegate的相应属性
		delegate.initDefaults(root, parentDelegate);
		return delegate;
	}

	/**
	 * 解析文档根级别的元素:
	 * "import", "alias", "bean".
	 *
	 * @param root the DOM root element of the document
	 */
	protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
		// 如果 node 隶属默认命名空间,走本分支,默认命名空间:"http://www.springframework.org/schema/beans"
		if (delegate.isDefaultNamespace(root)) {
			// 获得 根节点 的子节点，即<beans/>下的所有节点，不包括beans本身
			NodeList nl = root.getChildNodes();
			// 遍历子节点
			for (int i = 0; i < nl.getLength(); i++) {
				Node node = nl.item(i);
				// 如果子节点属于元素节点，走本分支,例<bean/>就是元素节点
				if (node instanceof Element) {
					Element ele = (Element) node;
					// 走本分支,如果当前元素隶属默认命名空间，例：<bean/>节点就会继承root节点的命名空间
					if (delegate.isDefaultNamespace(ele)) {
						parseDefaultElement(ele, delegate);
					}
					// 如果当前元素不隶属默认命名空间
					else {
						delegate.parseCustomElement(ele);
					}
				}
			}
		}
		// 如果node 不隶属默认名称空间。
		else {
			delegate.parseCustomElement(root);
		}
	}

	private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
		// 如果节点名是'import'
		if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
			importBeanDefinitionResource(ele);
		}
		// 如果节点名是'alias'
		else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
			processAliasRegistration(ele);
		}
		// 如果节点名是'bean'
		else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
			processBeanDefinition(ele, delegate);
		}
		// 如果节点名是'beans'
		else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
			// recurse
			doRegisterBeanDefinitions(ele);
		}
	}

	/**
	 * Parse an "import" element and load the bean definitions
	 * from the given resource into the bean factory.
	 */
	protected void importBeanDefinitionResource(Element ele) {
		// 获得'resource'属性,地表当前xml文件,import导入的xml文件路径
		String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
		if (!StringUtils.hasText(location)) {// 如果resource属性不包含值,则报错
			getReaderContext().error("Resource location must not be empty", ele);
			return;
		}

		// 如果import导入的xml文件路径包含占位符,则替换占位符 例如: "${user.dir}"
		// getReaderContext()返回 XmlReaderContext 实例
		// getEnvironment() 返回StandardEnvironment 实例
		location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);

		Set<Resource> actualResources = new LinkedHashSet<>(4);

		// 表示配置的xml文件路径是否绝对路径,例:classpath*:是绝对路径
		boolean absoluteLocation = false;
		try {
			// 判断配置的xml文件路径是绝对路径还是相对路径
			absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
		}
		catch (URISyntaxException ex) {
			// cannot convert to an URI, considering the location relative
			// unless it is the well-known Spring prefix "classpath*:"
		}

		// 如果是绝对路径,例:classpath*
		if (absoluteLocation) {
			try {
				// 递归调用 loadBeanDefinitions 方法,加载import导入进来的xml文件中的ben定义
				// getReaderContext() 返回 XmlReaderContext实例
				// getReader() 返回 XmlBeanDefinitionReader实例
				int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from URL location [" + location + "]");
				}
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error(
						"Failed to import bean definitions from URL location [" + location + "]", ele, ex);
			}
		}
		// 如果是相对路径
		else {
			// No URL -> considering resource location as relative to the current file.
			try {
				int importCount;
				Resource relativeResource = getReaderContext().getResource().createRelative(location);
				if (relativeResource.exists()) {
					importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
					actualResources.add(relativeResource);
				}
				else {
					String baseLocation = getReaderContext().getResource().getURL().toString();
					importCount = getReaderContext().getReader().loadBeanDefinitions(
							StringUtils.applyRelativePath(baseLocation, location), actualResources);
				}
				if (logger.isDebugEnabled()) {
					logger.debug("Imported " + importCount + " bean definitions from relative location [" + location + "]");
				}
			}
			catch (IOException ex) {
				getReaderContext().error("Failed to resolve current resource location", ele, ex);
			}
			catch (BeanDefinitionStoreException ex) {
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
			}
			catch (Exception ex) {
				getReaderContext().error("Failed to register alias '" + alias +
						"' for bean with name '" + name + "'", ele, ex);
			}
			getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
		}
	}

	/**
	 * Process the given bean element, parsing the bean definition
	 * and registering it with the registry.
	 */
	protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
		// 解析bean节点中的信息,创建BeanDefinitionHolder实例(是xml文件中bean定义信息的对应实体，也可称为bean定义信息的持有者实体)
		BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
		if (bdHolder != null) {
			// tofix 不常用，本Demo不涉及，暂不细讲
			// 对bean进行修饰 基于自定义属性
			bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
			try {
				// 注册最后的修饰实例。
				// getReaderContext() 返回 XmlReaderContext; getRegistry()返回 DefaultListableBeanFactory
				BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
			}
			catch (BeanDefinitionStoreException ex) {
				getReaderContext().error("Failed to register bean definition with name '" +
						bdHolder.getBeanName() + "'", ele, ex);
			}
			// 发送注册事件
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
	 * @see #getReaderContext()
	 */
	protected void postProcessXml(Element root) {
	}

}
