LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../mupdf $(LOCAL_PATH)/../../jpeg $(LOCAL_PATH)/../../pdfview2/include
LOCAL_CFLAGS := -Drestrict=
LOCAL_MODULE    := fitz
LOCAL_SRC_FILES := \
        base_cpudep.c \
        base_error.c \
        base_hash.c \
        base_matrix.c \
        base_memory.c \
        base_rect.c \
        base_string.c \
        base_unicode.c \
         \
	crypt_aes.c \
	crypt_arc4.c \
	crypt_crc32.c \
	crypt_md5.c \
         \
	obj_array.c \
	obj_dict.c \
	obj_parse.c \
	obj_print.c \
	obj_simple.c \
         \
	stm_buffer.c \
	stm_filter.c \
	apv_stm_open.c \
	apv_stm_read.c \
	stm_misc.c \
         \
    filt_basic.c \
	filt_pipeline.c \
	filt_arc4.c \
	filt_aesd.c \
         \
	filt_dctd.c \
	filt_faxd.c \
	filt_faxdtab.c \
	filt_flate.c \
	filt_lzwd.c \
	filt_predict.c \
         \
	node_toxml.c \
	node_misc1.c \
	node_misc2.c \
	node_path.c \
	node_text.c \
	node_tree.c \
         \
	res_colorspace.c \
	res_font.c \
	res_image.c \
	res_shade.c


include $(BUILD_STATIC_LIBRARY)
