#pragma version(1)
#pragma rs java_package_name(lvr.sidnapp)

//#include "rs_allocation.rsh"

short* data;
short* kernel;
float* res;

void convolve(int dataLength, int kernelLength) {

    //this is VERY slow, try FFT convolution there
    for ( int i = 0; i < dataLength; i++ ) {
        float sum = 0;
        for (int j = 0; j < kernelLength; j++) {
            if (i >= j) {
                sum += data[i-j] * kernel[j];
            }
        }
        res[i] = sum;
    }
}

/* void rsconv(rs_allocation data, rs_allocation kernel, rs_allocation res) {
    const int dataLength = rsAllocationGetDimX(data);
    const int kernelLength = rsAllocationGetDimX(kernel);

    res = rsCreateAllocation_short(dataLength);

    rs_allocation resf = rsCreateAllocation_float(dataLength);

    float max = -32768*dataLength;

    //this is VERY slow, try FFT convolution there
    for ( int i = 0; i < dataLength; i++ ) {
        float sum = 0;
        for (int j = 0; j < kernelLength; j++) {
            if (i >= j) {
                sum += data[i-j] * kernel[j];
            }
        }
        if (sum > max) {
            max = sum;
        }
        if (sum < -max) {
            max = -sum;
        }
        resf[i] = sum;
    }

    for (int i=0; i < dataLength; i++) {
        res[i] = (short) resf[i] / max * 32767;
    }
}*/