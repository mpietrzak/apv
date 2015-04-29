

# Introduction #

Short info about how to build libraries needed by APV and APV itself.

# Choosing lite or pro version #

There are two APV versions: Lite version and Pro version.

Both versions are open source and both are available at code.google.com/p/apv,
but Pro version is not free on Android Market and it has more features.

Current temporary method of switching source tree version is to use scripts/pjpp.py script.
To run this script you need Python 2.7.x or PyPy.

To switch to lite version, run:

```sh

python2.7 ./scripts/pjppy.py --configuration lite
```

To switch to pro version, run:

```sh

python2.7 ./scripts/pjpp.py --configuration pro
```


pjpp.py will rename various files and directories, replace some strings in source files and handle special
kind of comments inside files to comment or uncomment code specific to given version.

Code to be uncommented in version FOO is surrounded with comments:

```
// #ifdef FOO
// Log.d(TAG, "this code will be uncommented by pjpp.py --configuration FOO");
// #endif
```

or

```
<!-- #ifdef FOO -->
<!-- <strong>hello, foo!</strong> -->
<!-- #endif --->
```


pjpp.py script is a temporary solution. Quick binging suggests that better way
would be to use library projects and to split projects into three (or possibly more) subprojects:

  * apv base - code common to both versions,
  * apv lite - code specific to lite version that extends code found in apv base subproject,
  * apv pro - code specific to pro version that extends code found in apv base subproject.

However, this method requires a little bit more work than quick and dirty script preprocessing version, so for now
we're using pjpp.py.


After choosing version (pro or lite), code can be compiled in standard way, usually by importing to Eclipse.

You can switch versions as many times as you like after project is imported into Eclipse, but please remember to refresh
Eclipse after files are changed by pjpp.py.

APV source code repository contains lite version. Before committing your changes:

  * test both versions, so that changes in lite don't break pro and vice versa,
  * always switch to lite version before committing.


# Current building method #

As of July 24, 2011, the code in the repository includes tarballs for all the libraries used.  Running `scripts/build_native.sh` will then copy the files from these tarballs into the appropriate subdirectories of the jni directory, generate font files, and finally run `ndk-build`.  Your only responsibility is to ensure that your `android-ndk` directory is in the path.

Currently the code probably will not work with the latest mupdf library code--the source includes the 04/16/2011 mupdf snapshot.

This has been tested with cygwin and `android-ndk-r6`.

# Older building methods #

## FreeType ##

FreeType version tested: 2.4.2

http://www.freetype.org/

1. Edit builds/freetype.mk, change

```
FT_COMPILE = $(CC) $(ANSIFLAGS) $(FT_CFLAGS)
```

to

```
FT_COMPILE = $(CC) $(FT_CFLAGS)
```

This is because Android's Bionic is not ANSI compatible.

2. Edit android-gcc-setup.sh to point to your installation of the Android NDK, then source it

```
. /path-to-pdfview/scripts/android-gcc-setup.sh
```

3. Run ft's configure:

```
./configure --prefix="/target/dir" --host=arm-linux --disable-shared --enable-static
```

Make sure neither target dir nor source dir don't have spaces.

4. Run `make`

5. There should be libfreetype.a in ./objs/.libs. You can extract this .a file with `ar x libfreetype.a` and check if .o files really are ARM files.

6. Copy libfreetype.a to pdfview/jni/pdfview2/lib

7. Create pdfview/jni/pdfview2/include if it doesn't exist

8. Copy `freetype-2.4.2/include/*` to `pdfview/jni/pdfview2/include/`

TODO: skip unused ft modules to get smaller binary size

## JPEG ##

http://www.ijg.org/

jpeglib should be build with Android NDK. There's already Android.mk file for jpeg in pdfview/jni/jpeg.

1. Download jpegsrc.v8b.tar.gz from http://www.ijg.org/files/

2. Extract contents to pdfview/jni/jpeg

3. Copy jconfig.txt to jconfig.h. Currently APV seems to work with default settings, so no changes should be necessary.

## mupdf ##

http://mupdf.com/

1. Download and extract mupdf from http://ccxvii.net/mupdf/download/

2. Copy `mupdf/mudpf/*` to `pdfview/jni/mupdf/mupdf`

3. Copy `mupdf/fitz/*` to `pdfview/jni/mupdf/fitz`

4. Copy `mupdf/draw/*` to `pdfview/jni/mupdf/draw`

5. Copy apps,cmaps and fonts dirs to  pdfview/jni/mupdf

6. `make -f APV.mk font_files`