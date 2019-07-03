cmake_minimum_required(VERSION 3.4.1)

add_library(knock SHARED
            knock_recognizer.c AKMemory.c AKKnockCommon.c ak_buffer.c ak_fir_filter.c ak_knock_finder.c)

# Include libraries needed for knock lib
target_link_libraries(knock
                      android
                      log)