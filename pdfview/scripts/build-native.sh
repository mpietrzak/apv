#!/bin/sh
# make sure ndk-build is in path

SCRIPTDIR=`dirname $0`
MUPDF_FILE=mupdf-a242b4cf1123910c4dba18c75a77f28c5f6d8f33.tar.bz2
MUPDF=mupdf-a242b4cf1123910c4dba18c75a77f28c5f6d8f33
FREETYPE=freetype-2.4.10
OPENJPEG=openjpeg-1.5.1
JBIG2DEC=jbig2dec-0.11
JPEGSRC=jpegsrc.v8d.tar.gz
JPEGDIR=jpeg-8d

cd "$SCRIPTDIR/../deps"

echo "extracting deps"
tar xvf $FREETYPE.tar.bz2
tar xvf $JPEGSRC
tar xvf $MUPDF_FILE
tar xvf $OPENJPEG.tar.gz
tar xvf $JBIG2DEC.tar.gz
cp $OPENJPEG/libopenjpeg/*.[ch] ../jni/openjpeg/
echo '#define PACKAGE_VERSION' '"'$OPENJPEG'"' > ../jni/openjpeg/opj_config.h
cp $JPEGDIR/*.[ch] ../jni/jpeg/
cp $JBIG2DEC/* ../jni/jbig2dec/
for x in draw fitz pdf ; do
    cp -r $MUPDF/$x/*.[ch] ../jni/mupdf/$x/
done

echo "overwriting fitz.h with apv_fitz.h"
cp -r $FREETYPE/{src,include} ../jni/freetype/
cp ../jni/mupdf-apv/fitz/apv_fitz.h ../jni/mupdf/fitz/fitz.h
cd ..

echo "running ndk-build"
ndk-build

echo "build-native done"
