/**
 * Copyright (C) 2013 The Android Open Source Project
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.yyxx.support.volley.source.toolbox;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import cn.yyxx.support.volley.source.VolleyError;
import cn.yyxx.support.volley.source.toolbox.ImageLoader.ImageContainer;
import cn.yyxx.support.volley.source.toolbox.ImageLoader.ImageListener;

/**
 * Handles fetching an image from a URL as well as the life-cycle of the associated request.
 */
public class NetworkImageView extends ImageView {
    /**
     * The URL of the network image to load
     */
    private String mUrl;

    /**
     * Resource ID of the image to be used as a placeholder until the network image is loaded. Won't
     * be set at the same time as mDefaultImageBitmap.
     */
    private int mDefaultImageId;

    /**
     * Bitmap of the image to be used as a placeholder until the network image is loaded. Won't be
     * set at the same time as mDefaultImageId.
     */
    @Nullable
    Bitmap mDefaultImageBitmap;

    /**
     * Resource ID of the image to be used if the network response fails. Won't be set at the same
     * time as mErrorImageBitmap.
     */
    private int mErrorImageId;

    /**
     * Bitmap of the image to be used if the network response fails. Won't be set at the same time
     * as mErrorImageId.
     */
    @Nullable
    private Bitmap mErrorImageBitmap;

    /**
     * Local copy of the ImageLoader.
     */
    private ImageLoader mImageLoader;

    /**
     * Current ImageContainer. (either in-flight or finished)
     */
    private ImageContainer mImageContainer;

    public NetworkImageView(Context context) {
        this(context, null);
    }

    public NetworkImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets URL of the image that should be loaded into this view. Note that calling this will
     * immediately either set the cached image (if available) or the default image specified by
     * {@link NetworkImageView#setDefaultImageResId(int)} on the view.
     *
     * <p>NOTE: If applicable, {@link NetworkImageView#setDefaultImageResId(int)} or {@link
     * NetworkImageView#setDefaultImageBitmap} and {@link NetworkImageView#setErrorImageResId(int)}
     * or {@link NetworkImageView#setErrorImageBitmap(Bitmap)} should be called prior to calling
     * this function.
     *
     * <p>Must be called from the main thread.
     *
     * @param url         The URL that should be loaded into this ImageView.
     * @param imageLoader ImageLoader that will be used to make the request.
     */
    @MainThread
    public void setImageUrl(String url, ImageLoader imageLoader) {
        Threads.throwIfNotOnMainThread();
        mUrl = url;
        mImageLoader = imageLoader;
        // The URL has potentially changed. See if we need to load it.
        loadImageIfNecessary(/* isInLayoutPass= */ false);
    }

    /**
     * Sets the default image resource ID to be used for this view until the attempt to load it
     * completes.
     *
     * <p>This will clear anything set by {@link NetworkImageView#setDefaultImageBitmap}.
     */
    public void setDefaultImageResId(int defaultImage) {
        mDefaultImageBitmap = null;
        mDefaultImageId = defaultImage;
    }

    /**
     * Sets the default image bitmap to be used for this view until the attempt to load it
     * completes.
     *
     * <p>This will clear anything set by {@link NetworkImageView#setDefaultImageResId}.
     */
    public void setDefaultImageBitmap(Bitmap defaultImage) {
        mDefaultImageId = 0;
        mDefaultImageBitmap = defaultImage;
    }

    /**
     * Sets the error image resource ID to be used for this view in the event that the image
     * requested fails to load.
     *
     * <p>This will clear anything set by {@link NetworkImageView#setErrorImageBitmap}.
     */
    public void setErrorImageResId(int errorImage) {
        mErrorImageBitmap = null;
        mErrorImageId = errorImage;
    }

    /**
     * Sets the error image bitmap to be used for this view in the event that the image requested
     * fails to load.
     *
     * <p>This will clear anything set by {@link NetworkImageView#setErrorImageResId}.
     */
    public void setErrorImageBitmap(Bitmap errorImage) {
        mErrorImageId = 0;
        mErrorImageBitmap = errorImage;
    }

    /**
     * Loads the image for the view if it isn't already loaded.
     *
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */
    void loadImageIfNecessary(final boolean isInLayoutPass) {
        int width = getWidth();
        int height = getHeight();
        ScaleType scaleType = getScaleType();

        boolean wrapWidth = false, wrapHeight = false;
        if (getLayoutParams() != null) {
            wrapWidth = getLayoutParams().width == LayoutParams.WRAP_CONTENT;
            wrapHeight = getLayoutParams().height == LayoutParams.WRAP_CONTENT;
        }

        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        boolean isFullyWrapContent = wrapWidth && wrapHeight;
        if (width == 0 && height == 0 && !isFullyWrapContent) {
            return;
        }

        // if the URL to be loaded in this view is empty, cancel any old requests and clear the
        // currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setDefaultImageOrNull();
            return;
        }

        // if there was an old request in this view, check if it needs to be canceled.
        if (mImageContainer != null && mImageContainer.getRequestUrl() != null) {
            if (mImageContainer.getRequestUrl().equals(mUrl)) {
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mImageContainer.cancelRequest();
                setDefaultImageOrNull();
            }
        }

        // Calculate the max image width / height to use while ignoring WRAP_CONTENT dimens.
        int maxWidth = wrapWidth ? 0 : width;
        int maxHeight = wrapHeight ? 0 : height;

        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.

        // update the ImageContainer to be the new bitmap container.
        mImageContainer =
                mImageLoader.get(
                        mUrl,
                        new ImageListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                if (mErrorImageId != 0) {
                                    setImageResource(mErrorImageId);
                                } else if (mErrorImageBitmap != null) {
                                    setImageBitmap(mErrorImageBitmap);
                                }
                            }

                            @Override
                            public void onResponse(
                                    final ImageContainer response, boolean isImmediate) {
                                // If this was an immediate response that was delivered inside of a
                                // layout
                                // pass do not set the image immediately as it will trigger a
                                // requestLayout
                                // inside of a layout. Instead, defer setting the image by posting
                                // back to
                                // the main thread.
                                if (isImmediate && isInLayoutPass) {
                                    post(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    onResponse(response, /* isImmediate= */ false);
                                                }
                                            });
                                    return;
                                }

                                if (response.getBitmap() != null) {
                                    setImageBitmap(response.getBitmap());
                                } else if (mDefaultImageId != 0) {
                                    setImageResource(mDefaultImageId);
                                } else if (mDefaultImageBitmap != null) {
                                    setImageBitmap(mDefaultImageBitmap);
                                }
                            }
                        },
                        maxWidth,
                        maxHeight,
                        scaleType);
    }

    private void setDefaultImageOrNull() {
        if (mDefaultImageId != 0) {
            setImageResource(mDefaultImageId);
        } else if (mDefaultImageBitmap != null) {
            setImageBitmap(mDefaultImageBitmap);
        } else {
            setImageBitmap(null);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        loadImageIfNecessary(/* isInLayoutPass= */ true);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mImageContainer != null) {
            // If the view was bound to an image request, cancel it and clear
            // out the image from the view.
            mImageContainer.cancelRequest();
            setImageBitmap(null);
            // also clear out the container so we can reload the image if necessary.
            mImageContainer = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }
}
