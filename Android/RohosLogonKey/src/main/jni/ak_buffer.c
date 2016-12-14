//
//  ak_buffer.cpp
//  Knock-Knock
//
//  Created by Andrey Soloviev <xmacdev@gmail.com> on 27/01/14.
//  Copyright (c) 2014 Andrey Soloviev. All rights reserved.
//

#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include "ak_buffer.h"

void ak_buffer_init(ak_buffer_t *buffer, uint32_t max)
{
    buffer->count = 0;
    buffer->max = max;
    buffer->buffer = calloc(max, sizeof(CMAcceleration));
}

void ak_buffer_dealloc(ak_buffer_t *buffer)
{
    free(buffer->buffer);
    buffer->count = 0;
}

void ak_buffer_pop(ak_buffer_t *buffer, CMAcceleration *values)
{
    AKMemoryCopy(buffer->buffer, values, buffer->count, sizeof(CMAcceleration));
    ak_buffer_reset(buffer);
}

void ak_buffer_reset(ak_buffer_t *buffer)
{
    buffer->count = 0;
}

void ak_buffer_add_value(ak_buffer_t *buffer, CMAcceleration value)
{
    assert(buffer->count < buffer->max);
    if ( buffer->count >= buffer->max ) return;

    buffer->buffer[buffer->count] = value;
    buffer->count++;

}

bool ak_buffer_is_filled(ak_buffer_t buffer)
{
    return ( buffer.count >= buffer.max );
}
