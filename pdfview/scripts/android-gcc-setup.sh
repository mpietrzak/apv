
# adjust paths below and then run
# . ./android-gcc-setup.sh
# to setup build environment

export NDK=$HOME/fun/android/android-ndk
HOST_TYPE=darwin-x86
ANDROID_VERSION=3
LIBDIR=$NDK/build/platforms/android-$ANDROID_VERSION/arch-arm/usr/lib

export CFLAGS="-march=armv5te -mtune=xscale"
export CC=$NDK/toolchains/arm-eabi-4.4.0/prebuilt/$HOST_TYPE/bin/arm-eabi-gcc
export CPPFLAGS=-I$NDK/build/platforms/android-$ANDROID_VERSION/arch-arm/usr/include
export LDFLAGS="-L$LIBDIR -nostdlib -ldl -lc -lz $LIBDIR/crtbegin_static.o"


