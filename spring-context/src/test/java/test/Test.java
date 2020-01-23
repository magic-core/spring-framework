package test;

import org.springframework.Persion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.AbstractPropertyResolver;

public class Test {
	public static void main(String[] args) {
//		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"classpath*:applicationContext.xml"/*,"classpath*:applicationContext.xml","classpath*:test.xml"*/}, true, null);
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:applicationContext.xml");
		((AbstractPropertyResolver)(context.getEnvironment())).setRequiredProperties("");
		Persion persion = (Persion) context.getBean("persion");
		System.out.println(persion.getP());
//		for (String s : persion.getName()) {
//
//		System.out.println(s);
//		}
//		persion.test();
	}
}
