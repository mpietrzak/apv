LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../fitz $(LOCAL_PATH)/../../pdfview2/include
LOCAL_MODULE    := mupdf
LOCAL_CFLAGS := -DNOCJK
LOCAL_SRC_FILES := \
	pdf_crypt.c \
	apv_pdf_debug.c \
	pdf_lex.c \
	pdf_nametree.c \
	apv_pdf_open.c \
	pdf_parse.c \
	apv_pdf_repair.c \
	pdf_stream.c \
	pdf_xref.c \
	pdf_annot.c \
	pdf_outline.c \
	pdf_cmap.c \
	pdf_cmap_parse.c \
	pdf_cmap_load.c \
	pdf_cmap_table.c \
	pdf_fontagl.c \
	pdf_fontenc.c \
	pdf_unicode.c \
	pdf_font.c \
	pdf_type3.c \
	pdf_fontmtx.c \
	pdf_fontfile.c \
	pdf_function.c \
	pdf_colorspace1.c \
	pdf_colorspace2.c \
	pdf_image.c \
	pdf_pattern.c \
	pdf_shade.c \
	pdf_shade1.c \
	pdf_shade4.c \
	pdf_xobject.c \
	pdf_build.c \
	pdf_interpret.c \
	pdf_page.c \
	pdf_pagetree.c \
	pdf_store.c \
	font_misc.c \
	font_mono.c \
	font_sans.c \
	font_serif.c

#	cmap_tounicode.c
#	font_cjk.c \
	cmap_cns.c \
	cmap_gb.c cmap_japan.c cmap_korea.c



include $(BUILD_STATIC_LIBRARY)


# vim: set sts=8 sw=8 ts=8 noet:
