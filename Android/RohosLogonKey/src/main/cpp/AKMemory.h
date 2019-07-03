//
//  AKMemory.h
//  Knock-Knock
//
//  Created by Andrey Soloviev <xmacdev@gmail.com> on 23/01/14.
//  Copyright (c) 2014 Andrey Soloviev. All rights reserved.
//
#include <stdbool.h>
#include <stdint.h>

extern void AKMemoryArrayCopy(const void *src, int32_t src_offset, void *dst, int32_t dst_offset, int32_t length, size_t size);
extern void AKMemoryCopy(const void *src, void *dst, int32_t length, size_t size);
extern void AKMemoryClearArray(void *src, int32_t length, size_t size);

extern void AKMemoryCreateArrayArray(const void **array, int32_t length, size_t size, int count);
extern void AKMemoryReleaseArrayArray(void **array, int32_t length, size_t size, int count);

extern bool AKMemoryCompareBytes(unsigned char *bytes1, unsigned char *bytes2, int32_t length);

extern unsigned char *AKMemoryReverseBytesCopy(unsigned char *in, int len, int step);

extern int  AKMemoryGetBit(unsigned char *data, int pos);
extern void AKMemorySetBit(unsigned char *data, int pos, int val);

extern double **AKMemoryCreateArray2DD(int32_t length, int32_t deepLength);
extern void **AKMemoryCreateArray2D(int32_t length, int32_t deepLength, size_t size);
extern void AKMemoryReleaseArray2D(void **array, int32_t length, int32_t deepLength);
