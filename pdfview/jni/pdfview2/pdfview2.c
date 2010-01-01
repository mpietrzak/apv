
#include <string.h>
#include <jni.h>

#include "android/log.h"

#include "fitz.h"
#include "mupdf.h"


struct pdf_s {
	pdf_xref *xref;
	pdf_outline *outline;
};


typedef struct pdf_s pdf_t;


pdf_t* parse_pdf_bytes(unsigned char *bytes, size_t len);
jint* get_page_image_bitmap(pdf_t *pdf, int pageno, int zoom_pmil, int left, int top, int *blen, int *width, int *height);
pdf_t* get_pdf_from_this(JNIEnv *env, jobject this);
void get_size(JNIEnv *env, jobject size, int *width, int *height);
void save_size(JNIEnv *env, jobject size, int width, int height);
void fix_samples(unsigned char *bytes, unsigned int w, unsigned int h);
int get_page_size(pdf_t *pdf, int pageno, int *width, int *height);
void pdf_android_loghandler(const char *m);


/*
JNIEXPORT jint Java_cx_hell_android_pdfview_PDFView2Activity_parse_pdf(
		JNIEnv *env,
		jobject jthis,
		jbyteArray bytes) {
	return 0;
}
*/


JNIEXPORT void JNICALL
Java_cx_hell_android_pdfview_PDF_parseBytes(
		JNIEnv *env,
		jobject jthis,
		jbyteArray bytes) {
	__android_log_print(ANDROID_LOG_INFO, "cx.hell.android.pdfview", "parseBytes...");
	jclass this_class = (*env)->GetObjectClass(env, jthis);
	jfieldID pdf_field_id = (*env)->GetFieldID(env, this_class, "pdf_ptr", "I");
	jbyte *cbytes = NULL;
	cbytes = (*env)->GetByteArrayElements(env, bytes, NULL);
	size_t len = (*env)->GetArrayLength(env, bytes);
	__android_log_print(ANDROID_LOG_INFO, "cx.hell.android.pdfview", "got parameters, byte array has %d elements", (int)len);
	pdf_t *pdf = parse_pdf_bytes(cbytes, len);
	(*env)->SetIntField(env, jthis, pdf_field_id, (int)pdf);
	(*env)->ReleaseByteArrayElements(env, bytes, cbytes, 0);
}


JNIEXPORT jint JNICALL
Java_cx_hell_android_pdfview_PDF_getPageCount(
		JNIEnv *env,
		jobject this) {
	pdf_t *pdf = NULL;
    pdf = get_pdf_from_this(env, this);
	if (pdf == NULL) return -1;
	return pdf_getpagecount(pdf->xref);
}


JNIEXPORT jintArray JNICALL
Java_cx_hell_android_pdfview_PDF_renderPage(
        JNIEnv *env,
        jobject this,
        jint pageno,
        jint zoom,
        jint left,
        jint top,
        jobject size) {

    int blen;
    jint *buf; /* rendered page, freed before return, as bitmap */
    jintArray jints; /* return value */
    int *jbuf; /* pointer to internal jint */
    pdf_t *pdf; /* parsed pdf data, extracted from java's "this" object */
    int width, height;

    get_size(env, size, &width, &height);

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "jni renderPage(pageno: %d, zoom: %d, left: %d, right: %d, width: %d, height: %d) start",
            (int)pageno, (int)zoom,
            (int)left, (int)top,
            (int)width, (int)height);

    pdf = get_pdf_from_this(env, this);
    buf = get_page_image_bitmap(pdf, pageno, zoom, left, top, &blen, &width, &height);

    if (buf == NULL) return NULL;

    save_size(env, size, width, height);

    /* TODO: learn jni and avoid copying bytes ;) */
    jints = (*env)->NewIntArray(env, blen);
	jbuf = (*env)->GetIntArrayElements(env, jints, NULL);
    memcpy(jbuf, buf, blen);
    (*env)->ReleaseIntArrayElements(env, jints, jbuf, 0);
    free(buf);
    return jints;
}


JNIEXPORT jint JNICALL
Java_cx_hell_android_pdfview_PDF_getPageSize(
        JNIEnv *env,
        jobject this,
        jint pageno,
        jobject size) {
    int width, height, error;
    pdf_t *pdf;

    pdf = get_pdf_from_this(env, this);
    if (pdf == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "this.pdf is null");
        return 1;
    }

    error = get_page_size(pdf, pageno, &width, &height);
    if (error != 0) {
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "get_page_size error: %d", (int)error);
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "fitz error is:\n%s", fz_errorbuf);
        return 2;
    }

    save_size(env, size, width, height);
    return 0;
}


JNIEXPORT void JNICALL
Java_cx_hell_android_pdfview_PDF_freeMemory(
        JNIEnv *env,
        jobject this) {
    pdf_t *pdf = NULL;
	jclass this_class = (*env)->GetObjectClass(env, this);
	jfieldID pdf_field_id = (*env)->GetFieldID(env, this_class, "pdf_ptr", "I");

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "freeMemory()");
	pdf = (pdf_t*) (*env)->GetIntField(env, this, pdf_field_id);
	(*env)->SetIntField(env, this, pdf_field_id, 0);

    /* TODO: free memory :D */
}



pdf_t* get_pdf_from_this(JNIEnv *env, jobject this) {
    static jfieldID field_id = 0;
    static unsigned char field_id_cached = 0;
    pdf_t *pdf = NULL;
    if (! field_id_cached) {
        jclass this_class = (*env)->GetObjectClass(env, this);
        field_id = (*env)->GetFieldID(env, this_class, "pdf_ptr", "I");
        field_id_cached = 1;
        __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "cached pdf_ptr field id %d", (int)field_id);
    }
	pdf = (pdf_t*) (*env)->GetIntField(env, this, field_id);
    return pdf;
}


void get_size(JNIEnv *env, jobject size, int *width, int *height) {
    static jfieldID width_field_id = 0;
    static jfieldID height_field_id = 0;
    static unsigned char fields_are_cached = 0;
    if (! fields_are_cached) {
        jclass size_class = (*env)->GetObjectClass(env, size);
        width_field_id = (*env)->GetFieldID(env, size_class, "width", "I");
        height_field_id = (*env)->GetFieldID(env, size_class, "height", "I");
        fields_are_cached = 1;
        __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "cached Size fields");
    }
    *width = (*env)->GetIntField(env, size, width_field_id);
    *height = (*env)->GetIntField(env, size, height_field_id);
}


void save_size(JNIEnv *env, jobject size, int width, int height) {
    static jfieldID width_field_id = 0;
    static jfieldID height_field_id = 0;
    static unsigned char fields_are_cached = 0;
    if (! fields_are_cached) {
        jclass size_class = (*env)->GetObjectClass(env, size);
        width_field_id = (*env)->GetFieldID(env, size_class, "width", "I");
        height_field_id = (*env)->GetFieldID(env, size_class, "height", "I");
        fields_are_cached = 1;
        __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "cached Size fields");
    }
    (*env)->SetIntField(env, size, width_field_id, width);
    (*env)->SetIntField(env, size, height_field_id, height);
}


pdf_t* parse_pdf_bytes(unsigned char *bytes, size_t len) {
    pdf_t *pdf;
    fz_error error;
    pdf_xref *xref;

    pdf = (pdf_t*)malloc(sizeof(pdf_t));
    pdf->xref = NULL;
    pdf->outline = NULL;

    xref = pdf_newxref();
    error = pdf_loadxref_mem(xref, bytes, len);
    if (error) {
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "got err from pdf_loadxref_mem: %d", (int)error);
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "fz errors:\n%s", fz_errorbuf);
        return NULL;
    }

    pdf->xref = xref;

    return pdf;
}


double get_page_zoom(pdf_page *page, int max_width, int max_height) {
    double page_width, page_height;
    double zoom_x, zoom_y;
    double zoom;
    page_width = page->mediabox.x1 - page->mediabox.x0;
    page_height = page->mediabox.y1 - page->mediabox.y0;

    zoom_x = max_width / page_width;
    zoom_y = max_height / page_height;

    zoom = (zoom_x < zoom_y) ? zoom_x : zoom_y;

    return zoom;
}


/**
 * Get part of page as bitmap.
 * Parameters left, top, width and height are interprted after scalling, so if we have 100x200 page scalled by 25% and
 * request 0x0 x 25x50 tile, we should get 25x50 bitmap of whole page content.
 * Page size is currently MediaBox size: http://www.prepressure.com/pdf/basics/page_boxes, but probably shuld be TrimBox.
 * pageno is 0-based.
 */
jint* get_page_image_bitmap(pdf_t *pdf, int pageno, int zoom_pmil, int left, int top, int *blen, int *width, int *height) {
    unsigned char *bytes = NULL;
    fz_matrix ctm;
    fz_obj *obj = NULL;
    double zoom;
    int rotate = 0;
    fz_rect bbox;
    fz_error error = 0;
    pdf_page *page = NULL;
    fz_pixmap *image = NULL;
    fz_renderer *rast = NULL;
    static int runs = 0;

    if (runs == 0) {
        /* TODO: move to jni init */
        __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "setting mupdf log handler routine");
        pdf_setloghandler(pdf_android_loghandler);
    }

    zoom = (double)zoom_pmil / 1000.0;

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "get_page_image_bitmap(pageno: %d) start", (int)pageno);

    /* TODO: save renderer in pdf_t */
    error = fz_newrenderer(&rast, pdf_devicergb, 0, 1024 * 512);
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "fz_newrenderer -> %d", (int)error);

    /* TODO: cache pages in pdf_t */
    obj = pdf_getpageobject(pdf->xref, pageno+1);

    error = pdf_loadpage(&page, pdf->xref, obj);
    if (error) {
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "pdf_loadpage -> %d", (int)error);
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "fitz error is:\n%s", fz_errorbuf);
        return NULL;
    }

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "page mediabox: %.2f x %.2f  %.2f x %.2f",
            (float)(page->mediabox.x0),
            (float)(page->mediabox.y0),
            (float)(page->mediabox.x1),
            (float)(page->mediabox.y1)
        );

    /* zoom = get_page_zoom(page, *width, *height); */
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "zoom: %.2f", (float)zoom);

    ctm = fz_identity();
    ctm = fz_concat(ctm, fz_translate(-page->mediabox.x0, -page->mediabox.y1));
    ctm = fz_concat(ctm, fz_scale(zoom, -zoom));
    rotate += page->rotate;
    if (rotate != 0) ctm = fz_concat(ctm, fz_rotate(rotate));
    /*
    bbox = fz_transformaabb(ctm, page->mediabox);
    */

    bbox.x0 = left;
    bbox.y0 = top;
    bbox.x1 = left + *width;
    bbox.y1 = top + *height;


    /* apply clipping to bbox */
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "bbox after (tile): %.2f x %.2f  %.2f x %.2f",
            (float)(bbox.x0),
            (float)(bbox.y0),
            (float)(bbox.x1),
            (float)(bbox.y1)
        );

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "attempting to render tree");
    error = fz_rendertree(&image, rast, page->tree, ctm, fz_roundrect(bbox), 1);
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "fz_rendertree -> %d", (int)error);
    if (error) {
        fz_rethrow(error, "rendering failed");
        /* TODO: cleanup mem on error, so user can try to open many files without causing memleaks; also report errors nicely to user */
        return NULL;
    }

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "got image %d x %d, asked for %d x %d",
            (int)(image->w), (int)(image->h),
            *width, *height);

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "will now fix_samples()");
    fix_samples(image->samples, image->w, image->h);
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "fix_samples() done");

    /* TODO: shouldn't malloc so often; but in practice, those temp malloc-memcpy pairs don't cost that much */
    bytes = (unsigned char*)malloc(image->w * image->h * 4);
    memcpy(bytes, image->samples, image->w * image->h * 4);
    *blen = image->w * image->h * 4;
    *width = image->w;
    *height = image->h;
    pdf_droppage(page);
    fz_droppixmap(image);
    fz_droprenderer(rast);
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "cleanup done");

    runs += 1;
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "get_page_image_bitmap: %d-th execution finished", (int)runs);
    return (jint*)bytes;
}


/**
 * Reorder bytes in image data - convert from mupdf image to android image.
 * TODO: make it portable across different architectures (when they're released).
 */
void fix_samples(unsigned char *bytes, unsigned int w, unsigned int h) {
        unsigned char r,g,b,a;
        unsigned i = 0;
        for (i = 0; i < (w*h); ++i) {
                unsigned int o = i*4;
                a = bytes[o+0];
                r = bytes[o+1];
                g = bytes[o+2];
                b = bytes[o+3];
                bytes[o+0] = b; /* b */
                bytes[o+1] = g; /* g */
                bytes[o+2] = r; /* r */
                bytes[o+3] = a;
        }
}


int get_page_size(pdf_t *pdf, int pageno, int *width, int *height) {
    fz_error error = 0;
    pdf_page *page = 0;
    fz_obj *pageobj = NULL;
    fz_obj *sizeobj = NULL;
    fz_rect bbox;

    pageobj = pdf_getpageobject(pdf->xref, pageno+1);
    sizeobj = fz_dictgets(pageobj, "MediaBox");
    bbox = pdf_torect(sizeobj);
    *width = bbox.x1 - bbox.x0;
    *height = bbox.y1 - bbox.y0;
    return 0;
}


void pdf_android_loghandler(const char *m) {
    /* __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview.mupdf", m); */
}

