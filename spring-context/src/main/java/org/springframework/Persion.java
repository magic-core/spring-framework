package org.springframework;

import java.util.List;

public  class Persion {

	private List<String> name;
	private Persion1 p;
	public void test() {
		System.out.println("hello world!");
	}

	public List<String> getName() {
		return name;
	}

	public void setName(List<String> name) {
		this.name = name;
	}

	public Persion1 getP() {
		return p;
	}

	public void setP(Persion1 p) {
		this.p = p;
	}
}
