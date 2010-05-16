LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../mupdf/fitz $(LOCAL_PATH)/../mupdf/mupdf $(LOCAL_PATH)/include
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -lz -llog -L$(LOCAL_PATH)/lib -lfreetype
#out/apps/pdfview2/libpng.a
LOCAL_STATIC_LIBRARIES := mupdf fitz mupdf fitzdraw jpeg jbig2dec openjpeg 
LOCAL_MODULE    := pdfview2
LOCAL_SRC_FILES := pdfview2.c

include $(BUILD_SHARED_LIBRARY)
