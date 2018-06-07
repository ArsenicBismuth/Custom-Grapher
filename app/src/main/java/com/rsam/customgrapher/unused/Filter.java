package com.rsam.customgrapher.unused;;

public class Filter {
	private static final int ORDER = 32;
	private static final boolean IIR = false;
    private float[] inputs = {0};
	private float[] outputs = {0};
	private float[] a = {0};
	private float[] b = {0};
	private float temp = 0;

    public void addVal(int value) {
		// Shifting data
        int i;
		for(i = ORDER-1; i > 0; i--) {
			inputs[i] = inputs[i - 1];
			if (IIR) outputs[i] = outputs[i - 1];
		}

		// Get the new first data, so current input
		inputs[0] = value;

		// Calculate the convolution from koefs
		temp = 0;
		for (i = 0; i < ORDER; i++) temp += inputs[i] * b[i];
		if (IIR) for (i = 1; i < ORDER; i++) temp -= outputs[i] * a[i];

		// Result
		outputs[0] = temp;
    }

	public float getVal() {
		return outputs[0];
	}
}