//
//  AKMemory.cpp
//  Knock-Knock
//
//  Created by Andrey Soloviev <xmacdev@gmail.com> on 23/01/14.
//  Copyright (c) 2014 Andrey Soloviev. All rights reserved.
//

#include "AKMemory.h"

void AKMemoryCreateArrayArray(const void **array, int32_t length, size_t size, int count)
{
    int i;
	for (i=0; i<count; i++) {
        array[i] = calloc(length, size);
    }
}

void AKMemoryReleaseArrayArray(void **array, int32_t length, size_t size, int count)
{
    int i;
	for (i=0; i<count; i++) {
        free(array[i]);
    }
    free(array);
}

void AKMemoryArrayCopy(const void *src, int32_t src_offset, void *dst, int32_t dst_offset, int32_t length, size_t size)
{
    const void *shiftedSrc = &src[src_offset*size];
    void *shiftedDsc = &dst[dst_offset*size];
    size_t bytesLength = (size_t)(length)*size;
    memcpy(shiftedDsc, shiftedSrc, bytesLength);
}

void AKMemoryCopy(const void *src, void *dst, int32_t length, size_t size)
{
    AKMemoryArrayCopy(src, 0, dst, 0, length, size);
}

void AKMemoryClearArray(void *src, int32_t length, size_t size)
{
    int i;
	for (i=0; i<length; i++) {
        memset(&src[i], 0, size);
    }
}

unsigned char * AKMemoryReverseBytesCopy(unsigned char *in, int len, int step)
{
    int numOfBytes = (len-1)/8 + 1;
    unsigned char *out = calloc(numOfBytes, sizeof(unsigned char));
    int i;
    for (i=0; i<len; i++) {
        int val = AKMemoryGetBit(in,(i+step)%len);
        AKMemorySetBit(out,i,val);
    }
    return out;
}

int AKMemoryGetBit(unsigned char *data, int pos)
{
    int posByte = pos/8;
    int posBit = pos%8;
    unsigned char valByte = data[posByte];
    int valInt = valByte>>(8-(posBit+1)) & 0x0001;
    return valInt;
}

void AKMemorySetBit(unsigned char *data, int pos, int val)
{
    int posByte = pos/8;
    int posBit = pos%8;
    unsigned char oldByte = data[posByte];
    oldByte = (unsigned char) (((0xFF7F>>posBit) & oldByte) & 0x00FF);
    unsigned char newByte = (unsigned char) ((val<<(8-(posBit+1))) | oldByte);
    data[posByte] = newByte;
}

void ** AKMemoryCreateArray2D(int32_t length, int32_t deepLength, size_t size)
{
    void **array = calloc(length, sizeof(void *));
    int i;
    for ( i = 0; i < length; i++ ) {
        array[i] = calloc(deepLength, size);
    }
    return array;
}

double ** AKMemoryCreateArray2DD(int32_t length, int32_t deepLength)
{
    double **array = calloc(length, sizeof(double *));
    int i;
    for ( i = 0; i < length; i++ ) {
        array[i] = calloc(deepLength, sizeof(double));
    }
    return array;
}

void AKMemoryReleaseArray2D(void **array, int32_t length, int32_t deepLength)
{
    int i;
	for ( i = 0; i < length; i++ ) {
        free(array[i]);
    }

    free(array);
}

bool AKMemoryCompareBytes(unsigned char *bytes1, unsigned char *bytes2, int32_t length)
{
    int i;
	for (i = 0; i < length; i++) {
        if (bytes1[i] != bytes2[i]) {
            return false;
        }
    }
    return true;
}
