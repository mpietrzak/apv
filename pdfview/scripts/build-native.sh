#!/bin/sh
# make sure ndk-build is in path

SCRIPTDIR=`dirname $0`

cd $SCRIPTDIR/../deps
tar xvf freetype-2.3.11.tar.bz2
tar xvf jpegsrc.v8a.tar.gz
tar xvf mupdf-0.8.165-source.tar.gz
mv mupdf-0.8.165 mupdf
tar xvf openjpeg_v1_4_sources_r697.tgz
tar xvf jbig2dec-0.11.tar.gz
cp openjpeg_v1_4_sources_r697/libopenjpeg/*.[ch] ../jni/openjpeg/
echo '#define PACKAGE_VERSION "1.4r697apv"' > ../jni/openjpeg/opj_config.h
cp jpeg-8a/*.[ch] ../jni/jpeg/
cp jbig2dec-0.11/* ../jni/jbig2dec/
for x in draw fitz pdf ; do
    cp -r mupdf/$x/*.[ch] ../jni/mupdf/$x/
done
cp mupdf/scripts/jconfig.h ../jni/mupdf/pdf/
cp -r mupdf/fonts ../jni/mupdf/
cp -r freetype-2.3.11/{src,include} ../jni/freetype/
gcc -o ../scripts/fontdump mupdf/scripts/fontdump.c
cd ../jni/mupdf
mkdir generated 2> /dev/null
../../scripts/fontdump generated/font_base14.h fonts/*.cff
../../scripts/fontdump generated/font_droid.h fonts/droid/DroidSans.ttf
cd ..
ndk-build
