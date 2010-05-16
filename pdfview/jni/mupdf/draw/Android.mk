LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../mupdf $(LOCAL_PATH)/../fitz
LOCAL_CFLAGS := -Drestrict=
LOCAL_MODULE    := fitzdraw
LOCAL_SRC_FILES := \
        archx86.c \
        blendmodes.c \
        glyphcache.c \
        imagedraw.c \
        imagescale.c \
        imageunpack.c \
        meshdraw.c \
        pathfill.c \
        pathscan.c \
        pathstroke.c \
        porterduff.c

include $(BUILD_STATIC_LIBRARY)
