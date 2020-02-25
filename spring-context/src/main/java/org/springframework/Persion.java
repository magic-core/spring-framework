package org.springframework;

import java.util.List;

public  class Persion {

	private List<String> name;
	private PersionB persionB;
	public void test() {
		System.out.println("hello world!");
	}

	public List<String> getName() {
		return name;
	}

	public void setName(List<String> name) {
		this.name = name;
	}

	public PersionB getPersionB() {
		return persionB;
	}

	public void setPersionB(PersionB persionB) {
		this.persionB = persionB;
	}
}
