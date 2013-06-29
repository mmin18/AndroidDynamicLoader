/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bitmapfun.ui;

import sample.bitmapfun.R;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

import com.dianping.loader.MyResources;
import com.example.android.bitmapfun.util.ImageCache;
import com.example.android.bitmapfun.util.ImageFetcher;

/**
 * params: <br>
 * imageUrl : string
 */
public class ImageDetailFragment extends Fragment implements View.OnClickListener {
    private static final String IMAGE_CACHE_DIR = "images";

    private String mImageUrl;
    private ImageView mImageView;
    private ImageFetcher mImageFetcher;

    /**
     * Populate image using a url from extras, use the convenience factory method
     * {@link ImageDetailFragment#newInstance(String)} to create this fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	
    	MyResources res = MyResources.getResource(ImageGridFragment.class);
        final View v = res.inflate(getActivity(), R.layout.image_detail_fragment, container, false);
        mImageView = (ImageView) v.findViewById(R.id.imageView);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImageUrl = getActivity().getIntent().getStringExtra("imageUrl");

        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;

        // For this sample we'll use half of the longest width to resize our images. As the
        // image scaling ensures the image is larger than this, we should be left with a
        // resolution that is appropriate for both portrait and landscape. For best image quality
        // we shouldn't divide by 2, but this will use more memory and require a larger memory
        // cache.
        final int longest = (height > width ? height : width) / 2;

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(getActivity(), longest);
        mImageFetcher.addImageCache(getFragmentManager(), cacheParams);
        mImageFetcher.setImageFadeIn(false);
        if(!TextUtils.isEmpty(mImageUrl)) {
            mImageFetcher.loadImage(mImageUrl, mImageView);
        }

        // Set up activity to go full screen
        getActivity().getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);

        // Enable some additional newer visibility and ActionBar features to create a more
        // immersive photo viewing experience
        final ActionBar actionBar = getActivity().getActionBar();

        // Hide title text and set home as up
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        // Start low profile mode and hide ActionBar
        mImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        actionBar.hide();

        mImageView.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }
    
    /**
     * Set on the ImageView in the ViewPager children fragments, to enable/disable low profile mode
     * when the ImageView is touched.
     */
    @Override
    public void onClick(View v) {
    	getActivity().finish();
    }
}
