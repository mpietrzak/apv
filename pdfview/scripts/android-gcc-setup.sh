
# adjust paths below and then run
# . ./android-gcc-setup.sh
# to setup build environment

NDK=/cygdrive/c/fun/android/android-ndk-1.6_r1
ANDROID_VERSION=3
LIBDIR=$NDK/build/platforms/android-$ANDROID_VERSION/arch-arm/usr/lib

export CFLAGS="-march=armv5te -mtune=xscale"
export CC=$NDK/build/prebuilt/windows/arm-eabi-4.2.1/bin/arm-eabi-gcc
export CPPFLAGS=-I$NDK/build/platforms/android-$ANDROID_VERSION/arch-arm/usr/include
export LDFLAGS="-L$LIBDIR -nostdlib -ldl -lc -lz $LIBDIR/crtbegin_static.o"


