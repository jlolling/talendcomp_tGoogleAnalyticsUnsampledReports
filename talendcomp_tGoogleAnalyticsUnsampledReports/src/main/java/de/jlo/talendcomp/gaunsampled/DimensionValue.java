package de.jlo.talendcomp.gaunsampled;

public class DimensionValue {
	
	public String name;
	public String value;
	public int rowNum;
	
	@Override
	public String toString() {
		return "DIM #" + rowNum + " " + name + "=" + value;
	}

}