LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# $(LOCAL_PATH)/include/cairo
# -lcairo -lpixman-1 -

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../mupdf/fitz $(LOCAL_PATH)/../mupdf/mupdf $(LOCAL_PATH)/include
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -lz -llog -L$(LOCAL_PATH)/lib -lfontconfig -lfreetype -lxml2 out/apps/pdfview2/libpng.a
LOCAL_STATIC_LIBRARIES := mupdf fitz mupdf fitzdraw jpeg png
LOCAL_MODULE    := pdfview2
LOCAL_SRC_FILES := pdfview2.c

include $(BUILD_SHARED_LIBRARY)
