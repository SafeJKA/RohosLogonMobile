//
//  ak_fir_filter.h
//  Knock-Knock
//
//  Created by Andrey Soloviev <xmacdev@gmail.com> on 23/01/14.
//  Copyright (c) 2014 Andrey Soloviev. All rights reserved.
//


typedef struct {
    uint32_t count; // размерность фильтра
    double *coef;   // коэффициенты яильтра
    CMAcceleration *tail; // остаток от входных данных с прерыдущей фильтрации для осуществления непрерывного цикла
} ak_fir_filter_t;

extern void ak_fir_filter_init(ak_fir_filter_t *filter, double const *coef, uint32_t count);

// Фильтрация данных
extern void ak_fir_filter_process(CMAcceleration *input, uint32_t length, ak_fir_filter_t filter, CMAcceleration *output);

extern void ak_fir_filter_dealloc(ak_fir_filter_t filter);

extern void ak_fir_filter_reset(ak_fir_filter_t *filter);

