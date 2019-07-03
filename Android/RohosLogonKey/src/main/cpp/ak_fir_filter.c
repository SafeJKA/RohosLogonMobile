//
//  ak_fir_filter.c
//  Knock-Knock
//
//  Created by Andrey Soloviev <xmacdev@gmail.com> on 23/01/14.
//  Copyright (c) 2014 Andrey Soloviev. All rights reserved.
//

#include <stdint.h>
#include <stdlib.h>
#include "knock_recognizer.h"
#include "ak_fir_filter.h"
#include "AKMemory.h"

uint32_t ak_filter_tail_size(ak_fir_filter_t filter) {
    return filter.count - 1;
}

void ak_fir_filter_init(ak_fir_filter_t *filter, double const *coef, uint32_t count)
{
    filter->count = count;
    filter->coef = calloc(count, sizeof(double));
    AKMemoryCopy(coef, filter->coef, count, sizeof(double));

    uint32_t tail_size = ak_filter_tail_size(*filter);
    filter->tail = calloc(tail_size, sizeof(CMAcceleration));
}

void ak_fir_filter_dealloc(ak_fir_filter_t filter)
{
    if ( filter.coef ) {
        free(filter.coef);
    }

    if ( filter.tail ) {
        free(filter.tail);
    }
}

void ak_fir_filter_reset(ak_fir_filter_t *filter)
{
    int i;
	for ( i = 0; i < (filter->count-1); i++ ) {
        filter->tail[i].x = 0.0;
        filter->tail[i].y = 0.0;
        filter->tail[i].z = 0.0;
    }
}

// Фильтрация данных
void ak_fir_filter_process(CMAcceleration *input, uint32_t length, ak_fir_filter_t filter, CMAcceleration *output)
{
    uint32_t filter_order = filter.count;
    double *filter_coef = filter.coef;

    uint32_t tail_size = ak_filter_tail_size(filter);

    // Расширяем входную длину с учетом остатка от предыдущей фильтрации
    int32_t extended_length = length + filter_order;
    CMAcceleration *portion = calloc(extended_length, sizeof(CMAcceleration));

    // Собираем данные из остатка от предыдущей фильтрации + новые входные данные
    uint32_t tail_position = length - tail_size;
    AKMemoryArrayCopy(filter.tail, 0, portion, 0, tail_size, sizeof(CMAcceleration));
    AKMemoryArrayCopy(input, 0, portion, tail_size, length, sizeof(CMAcceleration));

    // Сохраняем последние n-элементов из вохдных данных для последующей обработки
    AKMemoryArrayCopy(input, tail_position, filter.tail, 0, tail_size, sizeof(CMAcceleration));

    CMAcceleration *filtered = calloc(extended_length, sizeof(CMAcceleration));

    // Стандартный алгоритм фильтрации высокочастотного фильтра
    int i, j;
    for (i = 0; i < length; i++) {
        CMAcceleration sum;
        sum.x = 0;
        sum.y = 0;
        sum.z = 0;
        for (j = 0; j < filter_order; j++) {
            sum.x += (double)portion[i + j].x * filter_coef[j];
            sum.y += (double)portion[i + j].y * filter_coef[j];
            sum.z += (double)portion[i + j].z * filter_coef[j];
        }
        filtered[i] = sum;
    }

    // Сохраняем результат в output
    AKMemoryArrayCopy(filtered, 0, output, 0, length, sizeof(CMAcceleration));

    free(filtered);
    free(portion);
}
