//
//  ak_knock_finder.cpp
//  Knock-Knock
//
//  Created by Andrey Soloviev <xmacdev@gmail.com> on 08/02/14.
//  Copyright (c) 2014 Ok, kap Inc. All rights reserved.
//

#include <stdlib.h>
#include <stdbool.h>
#include <android/log.h>
#include "ak_knock_finder.h"
#include "AKKnockCommon.h"

void ak_knock_finder_prepare_data(double *input, ak_knock_finder_t *finder);

//////////////////////////////////////////////////////////////////////////////////////
#pragma mark -

void ak_knock_finder_dealloc(ak_knock_finder_t *finder)
{
    free(finder->buffer);
    free(finder->tail);
}

void ak_knock_finder_init(ak_knock_finder_t *finder, uint32_t buffer_size,
		ak_knock_finder_settings_t *settings)
{
	finder->settings.min_amplitude = settings->min_amplitude;
    finder->settings.max_knock_samples = settings->max_knock_samples;

    finder->settings.min_space_between_knocks = settings->min_space_between_knocks;
    finder->settings.max_space_between_knocks = settings->max_space_between_knocks;

    finder->settings.min_spaces_before_knock = settings->min_spaces_before_knock;
    finder->settings.min_spaces_after_knock = settings->min_spaces_after_knock;
    finder->settings.max_knocks = settings->max_knocks;

	/*
	finder->settings.min_amplitude = AK_KNOCK_AMPLITUDE;
    finder->settings.max_knock_samples = AK_KNOCK_DURATION;

    finder->settings.min_space_between_knocks = AK_KNOCK_MIN_SPEED;
    finder->settings.max_space_between_knocks = AK_KNOCK_MAX_SPEED;

    finder->settings.min_spaces_before_knock = AK_BEFORE_KNOCK_DORMANT_DURATION;
    finder->settings.min_spaces_after_knock = AK_AFTER_KNOCK_DORMANT_DURATION;
    finder->settings.max_knocks = AK_KNOCK_MAX_KNOCKS;
	*/

    finder->buffer_size = buffer_size;
    finder->buffer = calloc(buffer_size, sizeof(double));

    finder->tail_size = finder->settings.max_knock_samples - 1;
    finder->tail = calloc(finder->tail_size, sizeof(double));

    ak_knock_finder_reset(finder);
}

void ak_knock_finder_reset(ak_knock_finder_t *finder)
{
    finder->process.spaces_before_knocks = 0;
    finder->process.spaces_after_knocks = 0;
    finder->process.space_between_knocks = 0;
    finder->process.knocks_count = 0;
    finder->process.knocks_samples = 0;

    finder->process.is_knock_processing = false;
    finder->process.is_knock_start_processing = false;
}

void ak_knock_finder_setup_data(double *input, ak_knock_finder_t *finder)
{
    ak_knock_finder_prepare_data(input, finder);
    finder->process.offset = 0;
}


//////////////////////////////////////////////////////////////////////////////////////
#pragma mark - Data processing

// Предварительная обработка данных
void ak_knock_finder_prepare_data(double *input, ak_knock_finder_t *finder)
{
    int32_t extended_size = finder->buffer_size + finder->tail_size;
    double *portion = calloc(extended_size, sizeof(double));

    /* Используем сохраненный остаток от предыдущей партии входных данных */
    AKMemoryArrayCopy(finder->tail, 0, portion, 0, finder->tail_size, sizeof(double));
    AKMemoryArrayCopy(input, 0, portion, finder->tail_size, finder->buffer_size, sizeof(double));

    /*
        Предварительная обработка заключается в том, чтобы из отфильтрованных данных сделать:
        - абсолютные значения амплитуд (избавляемся от отрицательных значений
        - обнуляем значения, которые меньше минимальной амплитуды
        - исправляем случайные нулевые значения, возникающие из-за низкой частоты дискретизации

        В результате мы получаем данные из нулевых значений, там где слишком малые амплитуды,
        и не нулевые значения из данных, которые необходимо обработать.
        По сути работаем только с нулями и единицами, так как дальнейшее значение амплитуды значения не имеет,
        так как при стуке, так и при шуме могут возникнуть одинаково большие и малые амплитуды.

        При большей частоте дискретизации можно было бы точнее оценивать изменение амплитуд,
        но максимальная частота дискретизации в телефоне не позволила в матлабе распознавать данные таким образом
     */


    double stored_value = 0;        // предыдущее обработанное значение
    int stored_value_index = 0;     // индекс предыдущего значения

    int zero_samples = 0;           // кол-во подряд идущих нулевых отсчетов

    int i;
    for ( i = 0; i < extended_size; i++) {
        double value = fabs(portion[i]);

        // Обнуляем значения меньше заданной амплитуды
        if ( (value < finder->settings.min_amplitude) /*|| (value > finder->settings.max_amplitude)*/ ) {
            value = 0;
        }

        portion[i] = value;

        // Суть этого процесса заключается в том, чтобы заменить нулевые значения не нулевыми, если предполагается,
        // что нулевое значение появилось из-за погрешности при дискретизации.
        // В матлабе данный метод выдал существеннуые показатели к прибавке распознавания

        if ( value > 0 ) {

            // Если перед текущем ненулевым значением было какое-то кол-во нулевых, то смотрим, возникли ли они по ошибке или нет
            if ( zero_samples > 0 ) {

                // Если кол-во нулевых значений и
                // кол-во отсчетов между текущим не нулевым значением и предыдущим не нулевым не больше,
                // чем максимальная длина отсчетов в одном стуке,
                // то заполняем нули сохраненным ранее не нулевым значением

                if ( (zero_samples <= finder->settings.max_knock_samples) &&
                    ((i - stored_value_index + 1) < finder->settings.max_knock_samples) ) {
                    int j;
                	for ( j = 0; j < zero_samples; j++) {
                        portion[i-j-1] = stored_value;
                    }
                }

                // обнуляем кол-во нулей
                zero_samples = 0;
            }

            // Сохраняем текущее значение
            stored_value = value;
            stored_value_index = i;

        } else {

            // Начинаем отслеживать кол-во нулей, после того, как встретилось ненулевое значение
            if ( stored_value > 0 ) {
                zero_samples++;
            }
        }
    }

    uint32_t tail_position = extended_size - finder->tail_size;
    AKMemoryArrayCopy(portion, tail_position, finder->tail, 0, finder->tail_size, sizeof(double));

    AKMemoryArrayCopy(portion, 0, finder->buffer, 0, finder->buffer_size, sizeof(double));

    free(portion);

}

// Распознавание стуков
bool ak_knock_finder_process(ak_knock_finder_t *finder)
{
    finder->knocks_found = 0;

    ak_knock_finder_process_t *process = &finder->process;
    ak_knock_finder_settings_t settings = finder->settings;


    // Циклически обрабатываем буффер с последнего обработанного элемента, записанного в process->offset
    for (; finder->process.offset < finder->buffer_size; process->offset++ ) {

        double value = finder->buffer[process->offset];

        //if(value > 0)
        //{
        //	__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "value %f", value);
        //}
        // Если обработка стуков еще не началась,
        if ( !process->is_knock_start_processing ) {
        	//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "!process->is_knock_start_processing");
            if ( 0 == value )  {
            	//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "value == 0");
                // В данном случае мы полагаем, что перед стуком не должно быть значений с высокой амплитудой,
                // иначе рассматриваем это как шум, поэтому мы подсчитываем кол-во нулей перед тем, как встретилось положительное значение
                // и если нулей не достаточно, то считаем это шумом и начинаем поиск следующего вхождения нулей
                process->spaces_before_knocks++;
            } else {
            	//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon",
            	//		"spase befor knoks %d; min spases %d",
            	//		process->spaces_before_knocks, settings.min_spaces_before_knock);
                // Если нулей достаточно, чтобы начать обработку стука, то устанавливаем флаг начала обработки
                if ( process->spaces_before_knocks >= settings.min_spaces_before_knock ) {
                	//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "begin process");
                    process->is_knock_start_processing = 1;     // выставляем флаг начала обработки данных
                    process->is_knock_processing = 1;           // и флаг процесса обработки стука
                    process->knocks_samples = 1;                // кол-во осчетов ненулевых данных выставляем в 1
                } else {
                    // нулей было не достаточно и мы начинаем все сначала
                    process->spaces_before_knocks = 0;
                }
            }
        } else {
        	//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "process->is_knock_start_processing");
            // Если идет обработка текущего стука
            if ( process->is_knock_processing ) {
            	//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "process->is_knock_processing");
                // и нам встретилось нулевое значение, значит произошел очередной стук
                // предполагаем что минимальная длина стука равна единице, так что дополнительных проверок не делается
                if ( 0 == value ) {
                	//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "value == 0");
                    process->knocks_count++;            // инкрементируем кол-во найденных стуков
                    process->is_knock_processing = 0;   // сбрасываем флаг обработки стука
                    process->space_between_knocks = 1;  // кол-во нулей между стуками
                    process->spaces_after_knocks = 1;   // нулей после стука, если это последний стук, то нулей должно быть достаточно, чтобы считать эти данные не шумом
                    process->knocks_samples = 0;        // сбрасываем кол-во отсчетов не нулевых данных
                    //__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "knock's found");
                } else {
                	//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "value != 0");
                    process->knocks_samples++;          // инкрементируем кол-во отсчетов с ненулевыми данными

                    // Если импульс с не нулевыми данными больше заданной длины стука, то считаем это шумом
                    //__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "knocks samples %d; max knocks %d",
                    //		process->knocks_samples, settings.max_knock_samples);
                    if ( process->knocks_samples > settings.max_knock_samples ) {
                        ak_knock_finder_reset(finder);
                    }
                }
            } else {

                // Если в данный момент стук не обрабатывается и встречается ноль, то это может быть конец серии стуков
                if ( 0 == value ) {
                    process->space_between_knocks++; // увеличиваем кол-во нулей между стуками, на случай, если серия стуков еще не закончена
                    process->spaces_after_knocks++; // увеличиваем кол-во нулей после текущего стука, на случай, если это стук последний

                    //__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "spaces %d; min spaces %d",
                    //		process->spaces_after_knocks, settings.min_spaces_after_knock);
                    // Если нулей больше, чем минимальное заданное кол-во, то серия стуков закончена
                    if ( process->spaces_after_knocks >= settings.min_spaces_after_knock ) {

                        // Если кол-во стуков лежит в диапазоне мин. и макс. заданных значений, то считаем, что стук найден и выходим из цикла
                        if ( (process->knocks_count >= settings.min_knocks) && (process->knocks_count <= settings.max_knocks) ) {
                            finder->knocks_found = process->knocks_count;
                            ak_knock_finder_reset(finder);
                            break;
                        } else {
                            // иначе сбрасываем все значения и начинаем все сначала
                            finder->knocks_found = 0;
                            ak_knock_finder_reset(finder);
                        }
                    }

                } else {

                    // Если встретилось не нулевое значение, то возможно это начало следующего стука
                    // но если перед этим было слишком большое или слишком малое кол-во нулей, то это все шум
                    if ( (process->space_between_knocks < settings.min_space_between_knocks) ||
                        (process->space_between_knocks > settings.max_space_between_knocks) ){
                        ak_knock_finder_reset(finder);
                    } else {
                        // иначе начинаем обработку очередного стука
                        process->is_knock_processing = 1;
                        process->knocks_samples = 1;
                    }

                }

            }
        }


    }
    //__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "knocks_found %d", finder->knocks_found);
    return (finder->knocks_found > 0);
}
