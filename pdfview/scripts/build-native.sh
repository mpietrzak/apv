#!/bin/sh

SCRIPTDIR=`dirname $0`
source $SCRIPTDIR/android-gcc-setup.sh
cd $SCRIPTDIR/..
if [ ! -d "deps" ]
then
    mkdir -p deps
fi
cd deps
DEPSDIR=$PWD

JNIDIR=$DEPSDIR/../jni
if [ ! -d "$JNIDIR/pdfview2/lib" ]
then
    mkdir -p $JNIDIR/pdfview2/lib
fi

if [ ! -d "$JNIDIR/pdfview2/include" ]
then
    mkdir -p $JNIDIR/pdfview2/include
fi


cd $DEPSDIR
MUPDFSRC="mupdf-latest.tar.gz"
FTSRC="freetype-2.3.11.tar.bz2"
JPEGSRC="jpegsrc.v8a.tar.gz"
MUPDF=$DEPSDIR/mupdf
FT=$DEPSDIR/freetype-2.3.11
JPEG=$DEPSDIR/jpeg-8a


echo "Downloading sources."
if [ ! -e "$MUPDFSRC" ]
then
    echo "Downloading mupdf..."
    wget http://ccxvii.net/mupdf/download/$MUPDFSRC -O $MUPDFSRC
fi
if [ ! -e "$FTSRC" ]
then
    echo "Downloading freetype..."
    wget http://mirror.lihnidos.org/GNU/savannah/freetype/$FTSRC -O $FTSRC
fi
if [ ! -e "$JPEGSRC" ]
then
    echo "Downloading jpeg..."
    wget http://www.ijg.org/files/$JPEGSRC -O $JPEGSRC
fi

rm -rf $MUPDF $FT $JPEG
tar -xzvf $MUPDFSRC
tar -xjvf $FTSRC
tar -xzvf $JPEGSRC

cd $FT
sed -i -e '/^FT_COMPILE/s/\$(ANSIFLAGS) //' builds/freetype.mk
./configure --prefix="$FT/install" --host=arm-linux --disable-shared --enable-static
make
make install
\cp install/lib/libfreetype.a $JNIDIR/pdfview2/lib
\cp -rf include/* $JNIDIR/pdfview2/include

cd $JPEG
\cp *.c $JNIDIR/jpeg
\cp *.h $JNIDIR/jpeg
\cp jconfig.txt $JNIDIR/jpeg/jconfig.h


unset CFLAGS
unset CC
unset CPPFLAGS
unset LDFLAGS


cd $MUPDF
\cp mupdf/*.c $JNIDIR/mupdf/mupdf
\cp mupdf/*.h $JNIDIR/mupdf/mupdf
\cp fitz/*.c $JNIDIR/mupdf/fitz
\cp fitz/*.h $JNIDIR/mupdf/fitz
\cp fitzdraw/*.c $JNIDIR/mupdf/fitzdraw
\cp -rf apps $JNIDIR/mupdf
\cp -rf cmaps $JNIDIR/mupdf
\cp -rf fonts $JNIDIR/mupdf
cd  $JNIDIR/mupdf/mupdf
make -f APV.mk font_files

cd $DEPSDIR/..
if [ ! -d "$NDK/apps/pdfview" ]
then
    mkdir -p $NDK/apps/pdfview 
fi
\cp -rf ndk-app/Application.mk $NDK/apps/pdfview/Application.mk
sed -i -e "/APP_PROJECT_PATH/s|\/cygdrive.*|$PWD|g"  $NDK/apps/pdfview/Application.mk

cd $NDK
make APP=pdfview


