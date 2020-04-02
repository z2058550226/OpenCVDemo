cmake_minimum_required(VERSION 3.4.1)

aux_source_directory(. DIR_SRCS)

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${DIR_SRCS})

find_package(OpenCV 4.2 REQUIRED java)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries(native-lib ${OpenCV_LIBRARIES})

target_link_libraries( # Specifies the target library.
        native-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})