package com.rsam.customgrapher;

import java.util.LinkedList;

public class Filter {
	private int order = 32;
	private boolean iir = false;
    private double[] inputs;
	private double[] outputs;
	private double[] a = {0};
	private double[] b = {0};

	public int buffSize = 256;							    // Length of the buffer linked list
//    public LinkedList<Double> buffer = new LinkedList<>();	// To store output continuously
//	public double[] bufferArr;
    private double[] buffer;
    private int ib = 0;         // Buffer pointer

    public Filter(int order, double[] b, double[] a, int buffSize) {
		this(order, b, a);
		this.buffSize = buffSize;
	}

    public Filter(int order, double[] b, int buffSize) {
		this(order, b);
		this.buffSize = buffSize;
	}

	// IIR if a is given
    public Filter(int order, double[] b, double[] a) {
		this.order = order;
		this.a = a;
		this.b = b;
		if (a.length != 0) this.iir = true;
		else if (a.length != order) throw (new IllegalArgumentException("Invalid a-array size."));
		if (b.length != order) throw (new IllegalArgumentException("Invalid b-array size."));
		initFilter();
	}

	// FIR
    public Filter(int order, double[] b) {
		this.order = order;
		this.b = b;
		this.iir = false;
		if (b.length != order) throw (new IllegalArgumentException("Invalid b-array size."));
		initFilter();
	}

	private void initFilter() {
		// From language spec, default value of an array of double is positive zero
		if (iir) outputs = new double[order];
		else outputs = new double[1];
		inputs = new double[order];
	}

	public void addArray(short[] arr) {
        addArray(arr, 1);
	}

	public void addArray(short[] arr, int downSample) {
        buffer = new double[buffSize];                  // Remake buffer every add array request
        ib = 0;
		int arrSize = arr.length;
		for (int i = 0; i < arrSize; i++) {
			if (i % downSample == 0) addVal(arr[i]);    // Add every x data
		}
	}

	public void addArray(int[] arr) {
		addArray(arr, 1);
	}

	public void addArray(int[] arr, int downSample) {
        buffer = new double[buffSize];                  // Remake buffer every add array request
        ib = 0;
		int arrSize = arr.length;
		for (int i = 0; i < arrSize; i++) {
			if (i % downSample == 0) addVal(arr[i]);    // Add every x data
		}
	}

	public void addVal(short value) {
		addVal((int) value);
	}

    public void addVal(int value) {
        double temp;

		// Shifting data
        int i;
		for(i = order - 1; i > 0; i--) {
			inputs[i] = inputs[i - 1];
			if (iir) outputs[i] = outputs[i - 1];
		}

		// Get the new first data, so current input
		inputs[0] = value;

		// Calculate the convolution from koefs
		temp = 0;
		for (i = 0; i < order; i++)
			temp += inputs[i] * b[i];

		if (iir) for (i = 1; i < order; i++)
			temp -= outputs[i] * a[i];

		// Result
//		Log.d("Filt ", String.valueOf(outputs[0]));
		outputs[0] = temp;
		addBuffer(outputs[0]);
    }

    private void addBuffer(double value) {
        if (ib < buffSize) buffer[ib++] = value;
	}

    // Get collected output
    public double[] getBuffer() {
        return buffer;
    }

//    private void addBuffer(double value) {
//        buffer.addFirst(value);
//        if (buffer.size() > buffSize) {
//            buffer.removeLast();
//    }

//	public double[] getBufferArr() {
//        LinkedList<Double> bufferTemp = new LinkedList<>(buffer); // Copy to avoid manipulation while buffer is edited
//        bufferArr = new double[buffSize];
//        int i = 0;
//        for (Double v : bufferTemp) bufferArr[i++] = v;
//		return bufferArr;
//	}

	public double getVal() { return outputs[0]; }

	// Get collected output
//	public LinkedList<Double> getBuffer() {
//		return buffer;
//	}

//	public double[] getArray() { return outputs; }

}