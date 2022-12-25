set(PROJNAME "fhe")
set(LIB_NAME ${PROJNAME}-lib)

cmake_minimum_required(VERSION 3.13)

set(PWD ${CMAKE_SOURCE_DIR})

project(${PROJNAME} VERSION 3.7.2 LANGUAGES CXX C)

##################### GMP Shared Lib #####################
set(GMP "gmplib")
add_library(${GMP} SHARED IMPORTED)
set_property(TARGET ${GMP}
             PROPERTY
             IMPORTED_LOCATION ${PWD}/lib/gmp/lib/shared/libgmp.so)
include_directories(${PWD}/lib/gmp/include/shared)
###########################################################

##################### GMP Static Lib ##################### -- NOT WORKING
#add_library(${GMP} STATIC IMPORTED)
#set_property(TARGET ${GMP}
#             PROPERTY
#             IMPORTED_LOCATION ${PWD}/lib/gmp/lib/static/libgmp.a)
#include_directories(${PWD}/lib/gmp/include/static)
###########################################################

##################### NTL Static Lib #####################
set(NTL "ntllib")
add_library(${NTL} STATIC IMPORTED)
set_property(TARGET ${NTL}
             PROPERTY
             IMPORTED_LOCATION ${PWD}/lib/ntl/lib/libntl.a)
include_directories(${PWD}/lib/ntl/include)
###########################################################

## Option 1
########### HEAAN Static Lib + CPP Compilation #################
set(HEAAN "heaanlib")
add_library(${HEAAN} STATIC IMPORTED)
set_property(TARGET ${HEAAN}
             PROPERTY
             IMPORTED_LOCATION ${PWD}/lib/heaan/lib/libHEAAN.a)
include_directories(${PWD}/lib/heaan/include)

add_library(${LIB_NAME}
        SHARED
        libheaan.cpp
        )

target_link_libraries( # Specifies the target library.
        ${LIB_NAME}
        # Links the target library to the log library included in the NDK.
        log
        ${GMP}
        ${NTL}
        ${HEAAN})
#############################################################

## Option 2
########## HEAAN + CPP Compilation together #################
#set(HEAAN "heaanlib")
#include_directories(${PWD}/heaan/include)
#file(GLOB all_SRCS
#        "${PWD}/heaan/src/*.cpp"
#        )
##include_directories(${PWD}/heaan/src/)
#
#add_library(${LIB_NAME}
#            SHARED
#            libheaan.cpp
#            ${all_SRCS}
#            )
#
#target_link_libraries( # Specifies the target library.
#        ${LIB_NAME}
#        # Links the target library to the log library included in the NDK.
#        log
#        ${GMP}
#        ${NTL})
############################################################