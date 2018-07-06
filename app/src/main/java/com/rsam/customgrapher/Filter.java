package com.rsam.customgrapher;

import java.util.LinkedList;

import static java.lang.Math.abs;

public class Filter {
	private int order = 1;
	private boolean iir = false;
	private LinkedList<Double> inputs = new LinkedList<>();
	private LinkedList<Double> outputs = new LinkedList<>();
	private double[] a = {0};
	private double[] b = {0};
	private boolean rectified = false;

	// Iteration using linked list is going to be converted, implementing an Iterable.
	// So iterating access would be similar to normal access of an array.

	private int buffSize = 4;							    // Length of the buffer linked list
	private LinkedList<Double> buffer = new LinkedList<>();
    private int ib = 0;         // Buffer pointer

    // Recommended by Android studio to be package-private which is no modifier.
	// No modifier means its access level is similar to private (Class only) with the addition of Package.
    // But no Subclass (class which extends superclass) access like public.
    Filter(int order, double[] b, double[] a, int buffSize, boolean rectified) {
        this(order, b, a, buffSize);
        this.rectified = rectified;
    }

    Filter(int order, double[] b, double[] a, int buffSize) {
		this(order, b, a);
//        initBuffer(buffSize);
	}

    Filter(int order, double[] b, int buffSize, boolean rectified) {
        this(order, b, buffSize);
        this.rectified = rectified;
    }

    Filter(int order, double[] b, int buffSize) {
		this(order, b);
//		initBuffer(buffSize);
	}

	// IIR if a is given
    Filter(int order, double[] b, double[] a) {
		this.order = order;
		this.a = a;
		this.b = b;
		if (a.length != 0) this.iir = true;
		else if (a.length != order) throw (new IllegalArgumentException("Invalid a-array size."));
		if (b.length != order) throw (new IllegalArgumentException("Invalid b-array size."));
//		init();
	}

	// FIR
    Filter(int order, double[] b) {
		this.order = order;
		this.b = b;
		this.iir = false;
		if (b.length != order) throw (new IllegalArgumentException("Invalid b-array size."));
//		init();
	}

//	private void init() {
//		// From language spec, default value of an array of double is positive zero
//		if (iir) outputs = new double[order];
//		else outputs = new double[1];
//		inputs = new double[order];
//	}

//    private void initBuffer(int buffSize) {
//        // Buffer initialized since the size is constant
//        this.buffSize = buffSize;
//        buffer = new double[buffSize];
//    }

	public <T> void addArray(LinkedList<T> arr) {
    	addArray(arr, 1);
	}

	public <T> void addArray(LinkedList<T> arr, int downSample) {
		int i = 0;
		int arrSize = arr.size();
		for (T val : arr) {
			if (i % downSample == 0) addVal((Double) val);    // Add every x data
			i++;
		}
	}

	public void addArray(short[] arr) {
		addArray(arr, 1);
	}

	public void addArray(short[] arr, int downSample) {
		int arrSize = arr.length;
		for (int i = 0; i < arrSize; i++) {
			if (i % downSample == 0) addVal((double) arr[i]);    // Add every x data
		}
	}

    public void addVal(double value) {
		double temp = 0;

		// Set the current value as x[0], auto shift
        if (rectified) inputs.addFirst(abs(value));
        else  inputs.addFirst(value);

		// Calculate the convolution from koefs
		int i = 0;
		for(double x : inputs) {
			if (i >= order) continue;
			temp += x * b[i];
			i++;
		}

		if (iir) {
			// For IIR, calculation indeed started at y[1] * a[1]
			i = 0;
			for(double y : outputs) {
				if (i >= order) continue;
				if (i != 0) temp -= y * a[i];
				i++;
			}
		}

		// Data automatically shifted
		outputs.addFirst(temp);

		while (inputs.size() > order) {
			inputs.removeLast();
		}
		while (outputs.size() > order) {
			outputs.removeLast();
		}

		// Result
		addBuffer(outputs.getFirst());
    }

    private void addBuffer(double value) {
        buffer.addFirst(value);
		while (buffer.size() > buffSize) {
			buffer.removeLast();
		}
	}

    // Get single data from compounded output and remove it
    public double getBuffer() {
    	try {
			return buffer.pollLast();    // Get and remove last, give null if empty
		} catch (Exception e) {
    		return -999;
		}
    }

    // Get newest output
    public double getVal() {
    	return outputs.getFirst();
    }

}