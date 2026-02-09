#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
BUILD_DIR=${BUILD_DIR:-"${ROOT_DIR}/build-ffmpeg"}
SOURCE_DIR="${BUILD_DIR}/sources"
OUTPUT_DIR=${OUTPUT_DIR:-"${ROOT_DIR}/dist/ffmpeg/android/armeabi-v7a"}
NUM_JOBS=${NUM_JOBS:-$(getconf _NPROCESSORS_ONLN)}

ANDROID_API=24
ANDROID_NDK_HOME=${ANDROID_NDK_HOME:-${NDK_HOME:-${NDK_ROOT:-""}}}
if [[ -z "${ANDROID_NDK_HOME}" ]]; then
  echo "ANDROID_NDK_HOME is not set. Please point it to Android NDK r21e." >&2
  exit 1
fi

HOST_TAG=${HOST_TAG:-""}
if [[ -z "${HOST_TAG}" ]]; then
  case "$(uname -s)" in
    Linux*) HOST_TAG="linux-x86_64" ;;
    Darwin*) HOST_TAG="darwin-x86_64" ;;
    *)
      echo "Unsupported host OS for NDK toolchain." >&2
      exit 1
      ;;
  esac
fi

TOOLCHAIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/${HOST_TAG}"
SYSROOT="${TOOLCHAIN}/sysroot"

export PATH="${TOOLCHAIN}/bin:${PATH}"

TARGET_CC="armv7a-linux-androideabi${ANDROID_API}-clang"
TARGET_CXX="armv7a-linux-androideabi${ANDROID_API}-clang++"
TARGET_AR="llvm-ar"
TARGET_RANLIB="llvm-ranlib"
TARGET_STRIP="llvm-strip"

COMMON_CFLAGS="--sysroot=${SYSROOT} -fPIC"
COMMON_LDFLAGS="--sysroot=${SYSROOT}"

mkdir -p "${SOURCE_DIR}" "${OUTPUT_DIR}"

fetch_repo() {
  local repo_url=$1
  local repo_dir=$2

  if [[ -d "${repo_dir}/.git" ]]; then
    return
  fi

  git clone --depth 1 --recurse-submodules --shallow-submodules "${repo_url}" "${repo_dir}"
}

build_x264() {
  local src_dir="${SOURCE_DIR}/x264"
  fetch_repo "https://code.videolan.org/videolan/x264.git" "${src_dir}"

  pushd "${src_dir}" >/dev/null
  make distclean >/dev/null 2>&1 || true
  ./configure \
    --prefix="${OUTPUT_DIR}" \
    --host=arm-linux-androideabi \
    --enable-static \
    --disable-cli \
    --enable-pic \
    --sysroot="${SYSROOT}" \
    --extra-cflags="${COMMON_CFLAGS}" \
    --extra-ldflags="${COMMON_LDFLAGS}" \
    CC="${TARGET_CC}" \
    CXX="${TARGET_CXX}" \
    AR="${TARGET_AR}" \
    RANLIB="${TARGET_RANLIB}" \
    STRIP="${TARGET_STRIP}"
  make -j"${NUM_JOBS}"
  make install
  popd >/dev/null
}

build_dav1d() {
  local src_dir="${SOURCE_DIR}/dav1d"
  fetch_repo "https://code.videolan.org/videolan/dav1d.git" "${src_dir}"

  local build_dir="${BUILD_DIR}/dav1d-build"
  rm -rf "${build_dir}"
  mkdir -p "${build_dir}"

  local cross_file="${build_dir}/android-armv7a.txt"
  cat > "${cross_file}" <<EOF
[binaries]
c = '${TARGET_CC}'
cpp = '${TARGET_CXX}'
ar = '${TARGET_AR}'
strip = '${TARGET_STRIP}'
pkgconfig = 'pkg-config'

[properties]
c_args = ['--sysroot=${SYSROOT}', '-fPIC']
c_link_args = ['--sysroot=${SYSROOT}']
needs_exe_wrapper = true

[host_machine]
system = 'android'
cpu_family = 'arm'
cpu = 'armv7-a'
endian = 'little'
EOF

  pushd "${build_dir}" >/dev/null
  meson setup "${src_dir}" \
    --cross-file "${cross_file}" \
    --prefix "${OUTPUT_DIR}" \
    --buildtype release \
    --default-library static
  ninja -j"${NUM_JOBS}"
  ninja install
  popd >/dev/null
}

build_libvpx() {
  local src_dir="${SOURCE_DIR}/libvpx"
  fetch_repo "https://chromium.googlesource.com/webm/libvpx" "${src_dir}"

  pushd "${src_dir}" >/dev/null
  make distclean >/dev/null 2>&1 || true
  ./configure \
    --prefix="${OUTPUT_DIR}" \
    --target=armv7-android-gcc \
    --sdk-path="${ANDROID_NDK_HOME}" \
    --disable-examples \
    --disable-tools \
    --disable-docs \
    --disable-unit-tests \
    --enable-vp8 \
    --enable-vp9 \
    --enable-pic \
    --disable-shared \
    --enable-static \
    --as=yasm \
    CC="${TARGET_CC}" \
    CXX="${TARGET_CXX}" \
    AR="${TARGET_AR}" \
    RANLIB="${TARGET_RANLIB}" \
    STRIP="${TARGET_STRIP}" \
    CFLAGS="${COMMON_CFLAGS}" \
    LDFLAGS="${COMMON_LDFLAGS}"
  make -j"${NUM_JOBS}"
  make install
  popd >/dev/null
}

build_ffmpeg() {
  local src_dir="${SOURCE_DIR}/ffmpeg"
  fetch_repo "https://github.com/FFmpeg/FFmpeg.git" "${src_dir}"

  pushd "${src_dir}" >/dev/null
  make distclean >/dev/null 2>&1 || true

  export PKG_CONFIG_LIBDIR="${OUTPUT_DIR}/lib/pkgconfig"
  export PKG_CONFIG_PATH="${PKG_CONFIG_LIBDIR}"

  ./configure \
    --prefix="${OUTPUT_DIR}" \
    --target-os=android \
    --arch=arm \
    --cpu=armv7-a \
    --cc="${TARGET_CC}" \
    --cxx="${TARGET_CXX}" \
    --ar="${TARGET_AR}" \
    --ranlib="${TARGET_RANLIB}" \
    --strip="${TARGET_STRIP}" \
    --sysroot="${SYSROOT}" \
    --enable-cross-compile \
    --disable-shared \
    --enable-static \
    --disable-programs \
    --disable-doc \
    --disable-debug \
    --disable-everything \
    --enable-avcodec \
    --enable-avutil \
    --enable-libx264 \
    --enable-libvpx \
    --enable-libdav1d \
    --enable-gpl \
    --enable-nonfree \
    --enable-encoder=libx264 \
    --enable-decoder=h264,hevc,libvpx_vp8,libvpx_vp9,libdav1d \
    --enable-parser=h264,hevc,vp8,vp9,av1 \
    --extra-cflags="${COMMON_CFLAGS} -I${OUTPUT_DIR}/include" \
    --extra-ldflags="${COMMON_LDFLAGS} -L${OUTPUT_DIR}/lib"

  make -j"${NUM_JOBS}"
  make install
  popd >/dev/null
}

build_x264
build_dav1d
build_libvpx
build_ffmpeg

echo "FFmpeg static build completed. Output: ${OUTPUT_DIR}"