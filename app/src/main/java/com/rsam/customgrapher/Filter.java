package com.rsam.customgrapher;

public class Filter {
	private int order = 32;
	private boolean iir = false;
    private double[] inputs;
	private double[] outputs;
	private double[] a = {0};
	private double[] b = {0};

	private int buffSize = 4;							    // Length of the buffer linked list
//  public LinkedList<Double> buffer = new LinkedList<>();	// To store output continuously
//	public double[] bufferArr;
    private double[] buffer;
    private int ib = 0;         // Buffer pointer

    // Recommended by Android studio to be package-private which is no modifier.
	// No modifier means its access level is similar to private (Class only) with the addition of Package.
    // But no Subclass (class which extends superclass) access like public.
    Filter(int order, double[] b, double[] a, int buffSize) {
		this(order, b, a);
		this.buffSize = buffSize;
	}

    Filter(int order, double[] b, int buffSize) {
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

    public void initBuffer(int buffSize) {
        // Buffer initialized since the size is constant
        this.buffSize = buffSize;
        buffer = new double[buffSize];
    }

	public void addArray(short[] arr) {
        addArray(arr, 1);
	}

	public void addArray(short[] arr, int downSample) {
//        buffer = new double[buffSize];                  // Remake buffer every add array request, creating lot of objects
        ib = 0;                                         // Restart buffer pointer
		int arrSize = arr.length;
		for (int i = 0; i < arrSize; i++) {
			if (i % downSample == 0) addVal(arr[i]);    // Add every x data
		}
	}

	public void addArray(int[] arr) {
		addArray(arr, 1);
	}

	public void addArray(int[] arr, int downSample) {
//        buffer = new double[buffSize];                  // Remake buffer every add array request, creating lot of objects
        ib = 0;                                         // Restart buffer pointer
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

    // Get individual output
    public double getVal() { return outputs[0]; }

}