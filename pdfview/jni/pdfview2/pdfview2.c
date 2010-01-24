
#include <string.h>
#include <jni.h>

#include "android/log.h"

#include "fitz.h"
#include "mupdf.h"


/**
 * Holds pdf info.
 */
struct pdf_s {
	pdf_xref *xref;
	pdf_outline *outline;
    int fileno; /* used only when opening by file descriptor */
};


typedef struct pdf_s pdf_t;


pdf_t* create_pdf_t();
pdf_t* parse_pdf_bytes(unsigned char *bytes, size_t len);
pdf_t* parse_pdf_file(const char *filename);
pdf_t* parse_pdf_fileno(int fileno);
jint* get_page_image_bitmap(pdf_t *pdf, int pageno, int zoom_pmil, int left, int top, int *blen, int *width, int *height);
pdf_t* get_pdf_from_this(JNIEnv *env, jobject this);
void get_size(JNIEnv *env, jobject size, int *width, int *height);
void save_size(JNIEnv *env, jobject size, int width, int height);
void fix_samples(unsigned char *bytes, unsigned int w, unsigned int h);
int get_page_size(pdf_t *pdf, int pageno, int *width, int *height);
void pdf_android_loghandler(const char *m);



JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved) {
    __android_log_print(ANDROID_LOG_INFO, "cx.hell.android.pdfview", "JNI_OnLoad");
    fz_cpudetect();
    fz_accelerate();
    /* pdf_setloghandler(pdf_android_loghandler); */
    return JNI_VERSION_1_2;
}



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


/**
 * Implementation of native method PDF.parseFile.
 * Opens file and parses at least some bytes - so it could take a while.
 * @param file_name file name to parse.
 */
JNIEXPORT void JNICALL
Java_cx_hell_android_pdfview_PDF_parseFile(
        JNIEnv *env,
        jobject jthis,
        jstring file_name) {
    const char *c_file_name = NULL;
    jboolean iscopy;
    jclass this_class;
    jfieldID pdf_field_id;
    pdf_t *pdf = NULL;


    c_file_name = (*env)->GetStringUTFChars(env, file_name, &iscopy);
	this_class = (*env)->GetObjectClass(env, jthis);
	pdf_field_id = (*env)->GetFieldID(env, this_class, "pdf_ptr", "I");
	pdf = parse_pdf_file(c_file_name);
    (*env)->ReleaseStringUTFChars(env, file_name, c_file_name);
	(*env)->SetIntField(env, jthis, pdf_field_id, (int)pdf);
}


/**
 * Create pdf_t struct from opened file descriptor.
 */
JNIEXPORT void JNICALL
Java_cx_hell_android_pdfview_PDF_parseFileDescriptor(
        JNIEnv *env,
        jobject jthis,
        jobject fileDescriptor) {
    int fileno;
    jclass this_class;
    jfieldID pdf_field_id;
    pdf_t *pdf = NULL;

	this_class = (*env)->GetObjectClass(env, jthis);
	pdf_field_id = (*env)->GetFieldID(env, this_class, "pdf_ptr", "I");
    fileno = get_descriptor_from_file_descriptor(env, fileDescriptor);
	pdf = parse_pdf_fileno(fileno);
	(*env)->SetIntField(env, jthis, pdf_field_id, (int)pdf);
}


/**
 * Implementation of native method PDF.getPageCount - return page count of this PDF file.
 * Returns -1 on error, eg if pdf_ptr is NULL.
 * @param env JNI Environment
 * @param this PDF object
 * @return page count or -1 on error
 */
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


/**
 * Free resources allocated in native code.
 */
JNIEXPORT void JNICALL
Java_cx_hell_android_pdfview_PDF_freeResources(
        JNIEnv *env,
        jobject this) {
    pdf_t *pdf = NULL;
	jclass this_class = (*env)->GetObjectClass(env, this);
	jfieldID pdf_field_id = (*env)->GetFieldID(env, this_class, "pdf_ptr", "I");

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "freeMemory()");
	pdf = (pdf_t*) (*env)->GetIntField(env, this, pdf_field_id);
	(*env)->SetIntField(env, this, pdf_field_id, 0);

    /* TODO: free memory referenced by pdf :D */

    /* pdf->fileno is dup()-ed in parse_pdf_fileno */
    if (pdf->fileno >= 0) close(pdf->fileno);
    free(pdf);
}



/**
 * Get pdf_ptr field value, cache field address as a static field.
 * @param env Java JNI Environment
 * @param this object to get "pdf_ptr" field from
 * @return pdf_ptr field value
 */
pdf_t* get_pdf_from_this(JNIEnv *env, jobject this) {
    static jfieldID field_id = 0;
    static unsigned char field_is_cached = 0;
    pdf_t *pdf = NULL;
    if (! field_is_cached) {
        jclass this_class = (*env)->GetObjectClass(env, this);
        field_id = (*env)->GetFieldID(env, this_class, "pdf_ptr", "I");
        field_is_cached = 1;
        __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "cached pdf_ptr field id %d", (int)field_id);
    }
	pdf = (pdf_t*) (*env)->GetIntField(env, this, field_id);
    return pdf;
}


/**
 * Get descriptor field value from FileDescriptor class, cache field offset.
 * This is undocumented private field.
 * @param env JNI Environment
 * @param this FileDescriptor object
 * @return file descriptor field value
 */
int get_descriptor_from_file_descriptor(JNIEnv *env, jobject this) {
    static jfieldID field_id = 0;
    static unsigned char is_cached = 0;
    if (!is_cached) {
        jclass this_class = (*env)->GetObjectClass(env, this);
        field_id = (*env)->GetFieldID(env, this_class, "descriptor", "I");
        is_cached = 1;
        __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "cached descriptor field id %d", (int)field_id);
    }
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "will get descriptor field...");
    return (*env)->GetIntField(env, this, field_id);
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


/**
 * Store width and height values into PDF.Size object, cache field ids in static members.
 * @param env JNI Environment
 * @param width width to store
 * @param height height field value to be stored
 * @param size target PDF.Size object
 */
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


/**
 * pdf_t "constructor": create empty pdf_t with default values.
 * @return newly allocated pdf_t struct with fields set to default values
 */
pdf_t* create_pdf_t() {
    pdf_t *pdf = NULL;
    pdf = (pdf_t*)malloc(sizeof(pdf_t));
    pdf->xref = NULL;
    pdf->outline = NULL;
    pdf->fileno = -1;
}


/**
 * Parse bytes into PDF struct.
 * @param bytes pointer to bytes that should be parsed
 * @param len length of byte buffer
 * @return initialized pdf_t struct; or NULL if loading failed
 */
pdf_t* parse_pdf_bytes(unsigned char *bytes, size_t len) {
    pdf_t *pdf;
    fz_error error;

    pdf = create_pdf_t();

    pdf->xref = pdf_newxref();
    error = pdf_loadxref_mem(pdf->xref, bytes, len);
    if (error) {
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "got err from pdf_loadxref_mem: %d", (int)error);
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "fz errors:\n%s", fz_errorbuf);
        /* TODO: free resources */
        return NULL;
    }

    error = pdf_decryptxref(pdf->xref);
    if (error) {
        return NULL;
    }

    if (pdf_needspassword(pdf->xref)) {
        int authenticated = 0;
        authenticated = pdf_authenticatepassword(pdf->xref, "");
        if (!authenticated) {
            /* TODO: ask for password */
            __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "failed to authenticate with empty password");
            return NULL;
        }
    }

    pdf->xref->root = fz_resolveindirect(fz_dictgets(pdf->xref->trailer, "Root"));
    fz_keepobj(pdf->xref->root);

    pdf->xref->info = fz_resolveindirect(fz_dictgets(pdf->xref->trailer, "Info"));
    fz_keepobj(pdf->xref->info);

    pdf->outline = pdf_loadoutline(pdf->xref);

    return pdf;
}


/**
 * Parse file into PDF struct.
 */
pdf_t* parse_pdf_file(const char *filename) {
    pdf_t *pdf;
    fz_error error;

    pdf = create_pdf_t();

    pdf->xref = pdf_newxref();
    error = pdf_loadxref(pdf->xref, (char*)filename); /* mupdf doesn't store nor modify filename; TODO: patch mupdf to use const or pass copy of filename */
    if (error) {
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "got err from pdf_loadxref: %d", (int)error);
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "fz errors:\n%s", fz_errorbuf);
        return NULL;
    }

    error = pdf_decryptxref(pdf->xref);
    if (error) {
        return NULL;
    }

    if (pdf_needspassword(pdf->xref)) {
        int authenticated = 0;
        authenticated = pdf_authenticatepassword(pdf->xref, "");
        if (!authenticated) {
            /* TODO: ask for password */
            __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "failed to authenticate with empty password");
            return NULL;
        }
    }

    pdf->xref->root = fz_resolveindirect(fz_dictgets(pdf->xref->trailer, "Root"));
    fz_keepobj(pdf->xref->root);
    pdf->xref->info = fz_resolveindirect(fz_dictgets(pdf->xref->trailer, "Info"));
    if (pdf->xref->info) fz_keepobj(pdf->xref->info);
    pdf->outline = pdf_loadoutline(pdf->xref);
    return pdf;
}


/**
 * Parse opened file into PDF struct.
 * @param file opened file descriptor
 */
pdf_t* parse_pdf_fileno(int fileno) {
    pdf_t *pdf;
    fz_error error;

    pdf = create_pdf_t();
    pdf->fileno = dup(fileno);

    pdf->xref = pdf_newxref();
    error = pdf_loadxref_fileno(pdf->xref, pdf->fileno);
    if (error) {
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "got err from pdf_loadxref_fileno: %d", (int)error);
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "fz errors:\n%s", fz_errorbuf);
        return NULL;
    }

    error = pdf_decryptxref(pdf->xref);
    if (error) {
        return NULL;
    }

    if (pdf_needspassword(pdf->xref)) {
        int authenticated = 0;
        authenticated = pdf_authenticatepassword(pdf->xref, "");
        if (!authenticated) {
            /* TODO: ask for password */
            __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "failed to authenticate with empty password");
            return NULL;
        }
    }

    pdf->xref->root = fz_resolveindirect(fz_dictgets(pdf->xref->trailer, "Root"));
    fz_keepobj(pdf->xref->root);
    pdf->xref->info = fz_resolveindirect(fz_dictgets(pdf->xref->trailer, "Info"));
    if (pdf->xref->info) fz_keepobj(pdf->xref->info);
    pdf->outline = pdf_loadoutline(pdf->xref);

    return pdf;
}


/**
 * Calculate zoom to best match given dimensions.
 * There's no guarantee that page zoomed by resulting zoom will fit rectangle max_width x max_height exactly.
 * @param max_width expected max width
 * @param max_height expected max height
 * @param page original page
 * @return zoom required to best fit page into max_width x max_height rectangle
 */
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

    zoom = (double)zoom_pmil / 1000.0;

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "get_page_image_bitmap(pageno: %d) start", (int)pageno);

    /* TODO: save renderer in pdf_t */
    error = fz_newrenderer(&rast, pdf_devicergb, 0, 1024 * 512);

    pdf_flushxref(pdf->xref, 0);

    /* TODO: cache pages in pdf_t */
    obj = pdf_getpageobject(pdf->xref, pageno+1);

    error = pdf_loadpage(&page, pdf->xref, obj);
    if (error) {
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "pdf_loadpage -> %d", (int)error);
        __android_log_print(ANDROID_LOG_ERROR, "cx.hell.android.pdfview", "fitz error is:\n%s", fz_errorbuf);
        return NULL;
    }

    /*
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "page mediabox: %.2f x %.2f  %.2f x %.2f",
            (float)(page->mediabox.x0),
            (float)(page->mediabox.y0),
            (float)(page->mediabox.x1),
            (float)(page->mediabox.y1)
        );
    */

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

    error = fz_rendertree(&image, rast, page->tree, ctm, fz_roundrect(bbox), 1);
    if (error) {
        fz_rethrow(error, "rendering failed");
        /* TODO: cleanup mem on error, so user can try to open many files without causing memleaks; also report errors nicely to user */
        return NULL;
    }

    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview", "got image %d x %d, asked for %d x %d",
            (int)(image->w), (int)(image->h),
            *width, *height);

    fix_samples(image->samples, image->w, image->h);

    /* TODO: shouldn't malloc so often; but in practice, those temp malloc-memcpy pairs don't cost that much */
    bytes = (unsigned char*)malloc(image->w * image->h * 4);
    memcpy(bytes, image->samples, image->w * image->h * 4);
    *blen = image->w * image->h * 4;
    *width = image->w;
    *height = image->h;
    pdf_droppage(page);
    fz_droppixmap(image);
    fz_droprenderer(rast);

    runs += 1;
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
    __android_log_print(ANDROID_LOG_DEBUG, "cx.hell.android.pdfview.mupdf", m);
}

