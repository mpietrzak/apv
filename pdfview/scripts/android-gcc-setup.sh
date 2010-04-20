
# adjust paths below and then run
# . ./android-gcc-setup.sh
# to setup build environment

export NDK=/cygdrive/d/Code/android/android-ndk-r3
HOST_TYPE=windows
ANDROID_VERSION=3
LIBDIR=$NDK/build/platforms/android-$ANDROID_VERSION/arch-arm/usr/lib

export CFLAGS="-march=armv5te -mtune=xscale"
export CC=$NDK/build/prebuilt/$HOST_TYPE/arm-eabi-4.2.1/bin/arm-eabi-gcc
export CPPFLAGS=-I$NDK/build/platforms/android-$ANDROID_VERSION/arch-arm/usr/include
export LDFLAGS="-L$LIBDIR -nostdlib -ldl -lc -lz $LIBDIR/crtbegin_static.o"


