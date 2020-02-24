package org.springframework;

import java.util.List;

public  class Persion {

	private List<String> name;
	private Persion1 persionB;
	public void test() {
		System.out.println("hello world!");
	}

	public List<String> getName() {
		return name;
	}

	public void setName(List<String> name) {
		this.name = name;
	}

	public Persion1 getPersionB() {
		return persionB;
	}

	public void setPersionB(Persion1 persionB) {
		this.persionB = persionB;
	}
}
