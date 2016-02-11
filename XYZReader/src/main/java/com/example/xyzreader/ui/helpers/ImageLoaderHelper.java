package com.example.xyzreader.ui.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

public class ImageLoaderHelper
{
    private static ImageLoaderHelper sInstance;
    private final LruCache<String, Bitmap> mImageCache = new LruCache<String, Bitmap>(20);
    private ImageLoader mImageLoader;
    private ImageLoaderHelper(Context applicationContext)
    {
        RequestQueue queue = Volley.newRequestQueue(applicationContext);
        ImageLoader.ImageCache imageCache = new ImageLoader.ImageCache()
        {
            @Override
            public Bitmap getBitmap(String key)
            {
                return mImageCache.get(key);
            }

            @Override
            public void putBitmap(String key, Bitmap value)
            {
                mImageCache.put(key, value);
            }
        };
        mImageLoader = new ImageLoader(queue, imageCache);
    }

    public static ImageLoaderHelper getInstance(Context context)
    {
        if(sInstance == null)
        {
            sInstance = new ImageLoaderHelper(context.getApplicationContext());
        }

        return sInstance;
    }

    public ImageLoader getImageLoader()
    {
        return mImageLoader;
    }
}