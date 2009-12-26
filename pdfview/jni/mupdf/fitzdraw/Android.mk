LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../mupdf $(LOCAL_PATH)/../fitz
LOCAL_CFLAGS := -DNOCJK -Drestrict=
LOCAL_MODULE    := fitzdraw
LOCAL_SRC_FILES := \
        archppc.c \
        archsparc.c \
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
        pixmap.c \
        porterduff.c \
        render.c

include $(BUILD_STATIC_LIBRARY)
