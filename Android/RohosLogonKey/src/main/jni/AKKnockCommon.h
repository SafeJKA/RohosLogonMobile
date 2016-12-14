//
//  AKKnockCommon.h
//  Knock-Knock
//
//  Created by Andrey Soloviev <xmacdev@gmail.com> on 23/01/14.
//  Copyright (c) 2014 Andrey Soloviev. All rights reserved.
//

// Частота дискретизации, выставляем максимальное значение, для лучшей обработки
//#define AK_KNOCK_ACCELEROMETER_SAMPLES_PER_SECONDS  200
//#define AK_KNOCK_ACCELEROMETER_BUFFER_SIZE          (AK_KNOCK_ACCELEROMETER_SAMPLES_PER_SECONDS/2)

// Минимальная амплитуда для обработки сигнала, все что меньше этого значение - обнуляется
// 0.1
#define AK_KNOCK_AMPLITUDE                          0.5

// Максимальное кол-во ненулевых отсчетов в одном стуке
#define AK_KNOCK_DURATION                           15
//#define AK_KNOCK_DURATION                           30
// Минимальное кол-во нулевых значений между стуками, чем меньше значение, тем быстрее может быть стук
// 7
#define AK_KNOCK_MIN_SPEED                          17
//#define AK_KNOCK_MIN_SPEED                          34

// Максимальное кол-во нулевых значений между стуками, чем больше значение, тем размеренней могут быть стуки
// 35
#define AK_KNOCK_MAX_SPEED                          35
//#define AK_KNOCK_MAX_SPEED                          70

// Максимальное кол-во обрабатываемых стуков
// 5
#define AK_KNOCK_MAX_KNOCKS                         3

// Минимальное кол-во нулевых значений перед / после стуков
// считаем, что перед стуком должно быть спокойное состояние телефона
#define AK_BEFORE_KNOCK_DORMANT_DURATION            100
//#define AK_BEFORE_KNOCK_DORMANT_DURATION            30

#define AK_AFTER_KNOCK_DORMANT_DURATION             (AK_KNOCK_MAX_SPEED * 2)

// Размерность высокочастотного фильтра и коэффициенты
#define AK_KNOCK_FILTER_LENGTH                      9
extern double const kAKFilterCoefValues[AK_KNOCK_FILTER_LENGTH];
