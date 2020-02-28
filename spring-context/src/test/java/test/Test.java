package test;

import org.springframework.demo.PersionA;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Test {
	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath*:applicationContext.xml");
		PersionA persion = (PersionA) context.getBean("persionA");
		System.out.println(persion.getPb());
	}
}
