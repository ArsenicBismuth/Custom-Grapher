package com.rsam.customgrapher.unused;;

public class Difference {
	private int difference = 0;
    private int preVal = 0;

    public void addVal(int value) {
		difference = value - preVal;
        preVal = value;
    }
	
	public int getVal() {
		return difference;
	}
}