#include <limits.h>
#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <unistd.h>
#include "knock_recognizer.h"
#include "ak_fir_filter.h"
#include "ak_knock_finder.h"
#include "AKKnockCommon.h"

static volatile bool is_started = false;

ak_fir_filter_t _filter; // Высокочастотный фильтр 9 порядка
ak_knock_finder_t _finder; // Структура для поиска кликов

jmethodID callBack;

////////////////////////////////////////////////////////////////////////////////////////////////////
JNIEXPORT jboolean JNICALL Java_com_rohos_logon1_NativeKnockRecognizer_initRecognizing(JNIEnv * env,
		jobject thisObj, jint inBuffSize);

JNIEXPORT void JNICALL Java_com_rohos_logon1_NativeKnockRecognizer_recognizeKnock(JNIEnv * env,
		jobject thisObj, jfloatArray inJNIBuffer);
////////////////////////////////////////////////////////////////////////////////////////////////////

jboolean JNICALL Java_com_rohos_logon1_NativeKnockRecognizer_initRecognizing(JNIEnv * env,
		jobject thisObj, jint inBuffSize)
{
	ak_fir_filter_init(&_filter, kAKFilterCoefValues, AK_KNOCK_FILTER_LENGTH);
	//ak_knock_finder_init(&_finder, AK_KNOCK_ACCELEROMETER_BUFFER_SIZE);

	ak_knock_finder_settings_t settings;
	int buff_size = (int)inBuffSize;

	if(buff_size == 100)
	{
		settings.min_amplitude = AK_KNOCK_AMPLITUDE;
		settings.max_knock_samples = 15; // AK_KNOCK_DURATION

		settings.min_space_between_knocks = 17; // AK_KNOCK_MIN_SPEED
		settings.max_space_between_knocks = 35; // AK_KNOCK_MAX_SPEED

		settings.min_spaces_before_knock = 100; // AK_BEFORE_KNOCK_DORMANT_DURATION
		settings.min_spaces_after_knock = 70; // AK_AFTER_KNOCK_DORMANT_DURATION
		settings.max_knocks = AK_KNOCK_MAX_KNOCKS;
	}
	else
	{
		settings.min_amplitude = AK_KNOCK_AMPLITUDE;
		settings.max_knock_samples = 30;

		settings.min_space_between_knocks = 34;
		settings.max_space_between_knocks = 70;

		settings.min_spaces_before_knock = 200;
		settings.min_spaces_after_knock = 140;
		settings.max_knocks = AK_KNOCK_MAX_KNOCKS;
	}



	ak_knock_finder_init(&_finder, buff_size, &settings);

	jclass thisClass = (*env)->GetObjectClass(env, thisObj);
	callBack = (*env)->GetMethodID(env, thisClass, "callback", "(I)V");

	jboolean result = true;
	return result;
}


void JNICALL Java_com_rohos_logon1_NativeKnockRecognizer_recognizeKnock(JNIEnv * env, jobject thisObj,
		jfloatArray inJNIBuffer)
{
	// Convert the incoming JNI jfloatarray to C's jfloat[]
	jfloat* inCBuffer = (*env)->GetFloatArrayElements(env, inJNIBuffer, 0);
	if(inCBuffer == NULL) return;
	jsize length = (*env)->GetArrayLength(env, inJNIBuffer);

	int buffer_size = length / 3;
	//CMAcceleration buffer_data[buffer_size];
	CMAcceleration *buffer_data = calloc(buffer_size, sizeof(CMAcceleration));

	int i, count;
	count = 0;
	for(i = 0; i < buffer_size; i++)
	{
		buffer_data[i].x = (double)inCBuffer[count++];
		buffer_data[i].y = (double)inCBuffer[count++];
		buffer_data[i].z = (double)inCBuffer[count++];
	}

	(*env)->ReleaseFloatArrayElements(env, inJNIBuffer, inCBuffer, 0);
	//(*env)->DeleteLocalRef(env, inJNIBuffer);


	// Фильтруем данные высокочастотным фильтром
	CMAcceleration *filter_output = calloc(buffer_size, sizeof(CMAcceleration));
	ak_fir_filter_process(buffer_data, buffer_size, _filter, filter_output);

	// Этот цикл берет максимальную амплитуду из трех осей коориднат из отфильтрованных данных
	// Можно всегда работать с осью Z, но этот вариант показал прибавку к распознаванию в MATLAB
	double *finder_input = calloc(buffer_size, sizeof(double));
	for (i = 0; i < buffer_size; i++)
	{
	   CMAcceleration axis = filter_output[i];
	   double max_value = fabs(axis.z);

	   if ( fabs(axis.x) > max_value ) max_value = fabs(axis.x);
	   if ( fabs(axis.y) > max_value ) max_value = fabs(axis.y);

	   finder_input[i] = max_value;

	   //__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "max_value %f", max_value);
	}

	// Устанавливаем входные данные + происходит предварительная обработка данных перед самим процессом распознавания
	ak_knock_finder_setup_data(finder_input, &_finder);

	// Распознаем стуки, если стук найден функция возвращает 1, далее вызываем ее еще раз для поиска следующего стука.
	while ( ak_knock_finder_process(&_finder) )
	{
		//uint32_t count = _finder.knocks_found;

		if(callBack != NULL)
		{
			jint jcount = _finder.knocks_found;
			(*env)->CallVoidMethod(env, thisObj, callBack, jcount);
		}
		//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "knock found %d", count);
	}

	//__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "exit");

	//for(i = 0; i < bufferSize; i++)
	//{
	//	__android_log_print(ANDROID_LOG_DEBUG, "rohoslogon", "x: %f; y: %f; z: %f", accSamples[i].x,
	//			buffer_data[i].y, buffer_data[i].z);
	//}

	free(buffer_data);
	free(filter_output);
	free(finder_input);
}
