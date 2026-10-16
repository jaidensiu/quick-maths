cmake_minimum_required(VERSION 3.22.1)
project(quickmaths_audio LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

set(SHARED_AUDIO_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../shared/audio)

add_library(quickmaths_audio SHARED
        AudioEngine.cpp
        JniBridge.cpp
        ${SHARED_AUDIO_DIR}/Mixer.cpp)

target_include_directories(quickmaths_audio PRIVATE ${SHARED_AUDIO_DIR})

target_compile_options(quickmaths_audio PRIVATE -Wall)

target_link_libraries(quickmaths_audio
        PRIVATE
        oboe::oboe
        android
        log)
