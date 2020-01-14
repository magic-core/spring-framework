package org.springframework;

import java.util.List;

public  class Persion1 {
	private List<String> name;
	public void test() {
		System.out.println("hello world!");
	}

	public List<String> getName() {
		return name;
	}

	public void setName(List<String> name) {
		this.name = name;
	}
}
