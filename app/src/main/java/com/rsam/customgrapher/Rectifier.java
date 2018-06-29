package com.rsam.customgrapher;

public class Rectifier {

	private int buffSize = 4;							    // Length of the buffer linked list
    private int output;
    private int[] buffer;
    private int ib = 0;         // Buffer pointer

    // Recommended by Android studio to be package-private which is no modifier.
	// No modifier means its access level is similar to private (Class only) with the addition of Package.
    // But no Subclass (class which extends superclass) access like public.
    Rectifier(int buffSize) {
		initBuffer(buffSize);
	}

    public void initBuffer(int buffSize) {
        // Buffer initialized since the size is constant
        this.buffSize = buffSize;
        buffer = new int[buffSize];
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
		// Result
//		Log.d("Filt ", String.valueOf(outputs[0]));
		output = abs(value);
		addBuffer(output);
    }

    public int abs(int value) {
    	if (value < 0) return value *= -1;
    	else return value;
	}

    private void addBuffer(int value) {
        if (ib < buffSize) buffer[ib++] = value;
	}

    // Get collected output
    public int[] getBuffer() {
        return buffer;
    }

    // Get individual output
    public int getVal() { return output; }

}