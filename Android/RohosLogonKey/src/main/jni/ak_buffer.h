//
//  ak_buffer.h
//  Knock-Knock
//
//  Created by Andrey Soloviev <xmacdev@gmail.com> on 27/01/14.
//  Copyright (c) 2014 Andrey Soloviev. All rights reserved.
//
#include <stdbool.h>
#include "knock_recognizer.h"

typedef struct {
    uint32_t count;
    uint32_t max;
    CMAcceleration *buffer;
} ak_buffer_t;

extern void ak_buffer_init(ak_buffer_t *buffer, uint32_t max);
extern void ak_buffer_dealloc(ak_buffer_t *buffer);

extern void ak_buffer_add_value(ak_buffer_t *buffer, CMAcceleration value);
extern bool ak_buffer_is_filled(ak_buffer_t buffer);
extern void ak_buffer_pop(ak_buffer_t *buffer, CMAcceleration *values);
extern void ak_buffer_reset(ak_buffer_t *buffer);

