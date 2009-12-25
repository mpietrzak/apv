#include "fitz.h"
#include "mupdf.h"


static void (*pdf_loghandler)(const char *s) = NULL;
static int reinit_pdflog = 0;


/*
 * Enable logging by setting environment variable MULOG to:
 *   (a)ll or a combination of
 *   (x)ref (r)src (f)ont (i)mage (s)hade (p)age
 *
 * eg. MULOG=fis ./x11pdf mytestfile.pdf
 */

enum
{
	PDF_LXREF = 1,
	PDF_LRSRC = 2,
	PDF_LFONT = 4,
	PDF_LIMAGE = 8,
	PDF_LSHADE = 16,
	PDF_LPAGE = 32
};

static inline void pdflog(int tag, char *name, char *fmt, va_list ap)
{
	static int flags = 128;
	static int level = 0;
	static int push = 1;
	int i;

	if (flags == 128 || reinit_pdflog)
	{
		char *s = getenv("MULOG");
        if (pdf_loghandler != NULL) s = "axrfisp";
		flags = 0;
		if (s)
		{
			if (strstr(s, "a"))
				flags |= 0xffff;
			if (strstr(s, "x"))
				flags |= PDF_LXREF;
			if (strstr(s, "r"))
				flags |= PDF_LRSRC;
			if (strstr(s, "f"))
				flags |= PDF_LFONT;
			if (strstr(s, "i"))
				flags |= PDF_LIMAGE;
			if (strstr(s, "s"))
				flags |= PDF_LSHADE;
			if (strstr(s, "p"))
				flags |= PDF_LPAGE;
		}
        reinit_pdflog = 0;
	}

	if (!(flags & tag))
		return;

	if (strchr(fmt, '}'))
		level --;

	if (push)
	{
        if (pdf_loghandler == NULL) {
            printf("%s: ", name);
            for (i = 0; i < level; i++)
                printf("  ");
        }
	}

    if (pdf_loghandler == NULL) {
        vprintf(fmt, ap);
    } else {
        char b[2048];
        int ll = push ? level : 0;
        for(i = 0; i < ll; ++i) b[i] = ' ';
        vsnprintf(b+ll, 2048-ll, fmt, ap);
        b[2047] = 0;
        pdf_loghandler(b);
    }

	if (strchr(fmt, '{'))
		level ++;

	push = !!strchr(fmt, '\n');

    if (pdf_loghandler == NULL)
        fflush(stdout);
}

void pdf_logxref(char *fmt, ...)
{va_list ap;va_start(ap,fmt);pdflog(PDF_LXREF,"xref",fmt,ap);va_end(ap);}

void pdf_logrsrc(char *fmt, ...)
{va_list ap;va_start(ap,fmt);pdflog(PDF_LRSRC,"rsrc",fmt,ap);va_end(ap);}

void pdf_logfont(char *fmt, ...)
{va_list ap;va_start(ap,fmt);pdflog(PDF_LFONT,"font",fmt,ap);va_end(ap);}

void pdf_logimage(char *fmt, ...)
{va_list ap;va_start(ap,fmt);pdflog(PDF_LIMAGE,"imag",fmt,ap);va_end(ap);}

void pdf_logshade(char *fmt, ...)
{va_list ap;va_start(ap,fmt);pdflog(PDF_LSHADE,"shad",fmt,ap);va_end(ap);}

void pdf_logpage(char *fmt, ...)
{va_list ap;va_start(ap,fmt);pdflog(PDF_LPAGE,"page",fmt,ap);va_end(ap);}

void pdf_setloghandler(void (*newloghandler)(const char *)) {
    pdf_loghandler = newloghandler;
    reinit_pdflog = 1;
}
