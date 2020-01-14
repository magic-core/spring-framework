package test;

import org.springframework.Persion;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test {
	//
	public static void main(String[] args) {
//		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"classpath*:applicationContext.xml"/*,"classpath*:applicationContext.xml","classpath*:test.xml"*/}, true, null);
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:applicationContext.xml");
		Persion persion = (Persion) context.getBean("persion");
		persion.test();
	}
}
