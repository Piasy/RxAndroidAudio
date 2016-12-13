# cmake 最低版本要求
cmake_minimum_required(VERSION 3.4.1)

# 声明我们的模块名 audio-processor，打包目标，so 库 SHARED
# 以及源文件列表
add_library(audio-processor SHARED
            smbPitchShift.c)

# so 库需要包含的模块
target_link_libraries(audio-processor
            android
            log)
