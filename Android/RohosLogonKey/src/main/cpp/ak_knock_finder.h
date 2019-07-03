//
//  ak_knock_finder.h
//  Knock-Knock
//
//  Created by Andrey Soloviev <xmacdev@gmail.com> on 08/02/14.
//  Copyright (c) 2014 Ok, kap Inc. All rights reserved.
//

#include <stdint.h>
#include <stdbool.h>

// Настройки распознавания
typedef struct {
    uint32_t max_knock_samples;             // максимальное кол-во отсчетов в стуке

    uint32_t min_space_between_knocks;      // минимальное кол-во отсчетов с нулевыми данными между стуками
    uint32_t max_space_between_knocks;      // максимальное кол-во отсчетов с нулевыми данными между стуками
    uint32_t min_spaces_before_knock;       // минимальное кол-во отсчетов с нулевыми данными перед стуком
    uint32_t min_spaces_after_knock;        // минимальное кол-во отсчетов с нулевыми данными после стука

    uint32_t max_knocks;                    // максимальное кол-во стуков
    uint32_t min_knocks;                    // минимальное кол-во стуков
    double min_amplitude;                   // минимальная амплитуда, после которой данные считаются не нулевыми
    
} ak_knock_finder_settings_t;

// Текущий статус процесса распознавания
typedef struct {
    uint32_t offset;                        // Текущий индекс текущего обрабатываемого элемента
    uint32_t spaces_before_knocks;          // Текущее кол-во отсчетов с нулевыми данными перед стуком
    uint32_t spaces_after_knocks;           // Текущее кол-во отсчетов с нулевыми данными после стука
    uint32_t space_between_knocks;          // Текущее кол-во отсчетов с нулевыми данными между стуками
    uint32_t knocks_count;                  // Текущее кол-во найденных стуков
    uint32_t knocks_samples;                // Кол-во отсчетов в текущем стуке
    
    bool is_knock_processing;               // Идет ли обработка очередного стука
    bool is_knock_start_processing;         // Началась ли обработка данных серии стуков

} ak_knock_finder_process_t;


// Рабочая структура
typedef struct {
    double *buffer;                         // Буффер с входными фильтрованными данными
    uint32_t buffer_size;

    double *tail;                           // Остаток от предыдущей обработки для осуществления непрерывного процесса
    uint32_t tail_size;
    
    uint32_t knocks_found;                  // Кол-во найденных стуков
    ak_knock_finder_process_t process;      // Структура с текущем статусов обработки данных
    ak_knock_finder_settings_t settings;    // Настройки распознавания
    
} ak_knock_finder_t;


extern void ak_knock_finder_init(ak_knock_finder_t *finder, uint32_t buffer_size, ak_knock_finder_settings_t *settings);
extern void ak_knock_finder_dealloc(ak_knock_finder_t *finder);

void ak_knock_finder_reset(ak_knock_finder_t *finder);

// Предварительная обработка данных
extern void ak_knock_finder_setup_data(double *input, ak_knock_finder_t *finder);

// Распознавание данных
extern bool ak_knock_finder_process(ak_knock_finder_t *finder);
