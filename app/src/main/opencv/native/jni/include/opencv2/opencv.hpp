/*M///////////////////////////////////////////////////////////////////////////////////////
//
//  IMPORTANT: READ BEFORE DOWNLOADING, COPYING, INSTALLING OR USING.
//
//  By downloading, copying, installing or using the software you agree to this license.
//
//                           License Agreement
//                For Open Source Computer Vision Library
//
// Copyright (C) 2000-2008, Intel Corporation, all rights reserved.
// Copyright (C) 2009-2010, Willow Garage Inc., all rights reserved.
// Third party copyrights are property of their respective owners.
//
// Redistribution and use in source and binary forms, with or without modification,
// are permitted provided that the following conditions are met:
//
//   * Redistribution's of source code must retain the above copyright notice,
//     this list of conditions and the following disclaimer.
//
//   * In binary form, reproduce the above copyright notice, this list of conditions
//     and the following disclaimer in the documentation and/or other materials.
//
//   * The name of the copyright holders may not be used to endorse or promote products
//     derived from this software without specific prior written permission.
//
// This software is provided by the copyright holders and contributors "as is" and
// any express or implied warranties, including, but not limited to, the implied
// warranties of merchantability and fitness for a particular purpose are disclaimed.
//
M*/

#ifndef OPENCV_ALL_HPP
#define OPENCV_ALL_HPP

// Core library is always included
#include "opencv2/core.hpp"

// Minimal modules you are actually using
#ifdef HAVE_OPENCV_IMGPROC
#include "opencv2/imgproc.hpp"
#endif

#ifdef HAVE_OPENCV_IMGCODECS
#include "opencv2/imgcodecs.hpp"
#endif

// =====================================================
// The following modules are NOT used in your project
// =====================================================
//#ifdef HAVE_OPENCV_CALIB3D
//#include "opencv2/calib3d.hpp"
//#endif
//#ifdef HAVE_OPENCV_FEATURES2D
//#include "opencv2/features2d.hpp"
//#endif
//#ifdef HAVE_OPENCV_DNN
//#include "opencv2/dnn.hpp"
//#endif
//#ifdef HAVE_OPENCV_FLANN
//#include "opencv2/flann.hpp"
//#endif
//#ifdef HAVE_OPENCV_HIGHGUI
//#include "opencv2/highgui.hpp"
//#endif
//#ifdef HAVE_OPENCV_ML
//#include "opencv2/ml.hpp"
//#endif
//#ifdef HAVE_OPENCV_OBJDETECT
//#include "opencv2/objdetect.hpp"
//#endif
//#ifdef HAVE_OPENCV_PHOTO
//#include "opencv2/photo.hpp"
//#endif
//#ifdef HAVE_OPENCV_STITCHING
//#include "opencv2/stitching.hpp"
//#endif
//#ifdef HAVE_OPENCV_VIDEO
//#include "opencv2/video.hpp"
//#endif
//#ifdef HAVE_OPENCV_VIDEOIO
//#include "opencv2/videoio.hpp"
//#endif

#endif
