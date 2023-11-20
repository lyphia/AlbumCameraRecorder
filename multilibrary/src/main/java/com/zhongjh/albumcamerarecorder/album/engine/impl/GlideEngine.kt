package com.zhongjh.albumcamerarecorder.album.engine.impl

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.zhongjh.albumcamerarecorder.album.engine.ImageEngine
import com.zhongjh.common.utils.ActivityUtils.assertValidRequest

/**
 * [ImageEngine] implementation using Glide.
 * @author zhongjh
 */
class GlideEngine : ImageEngine {

    override fun loadThumbnail(
        context: Context,
        resize: Int,
        placeholder: Drawable,
        imageView: ImageView,
        path: String
    ) {
        Glide.with(context)
            .load(path)
            .asBitmap() // some .jpeg files are actually gif
            .placeholder(placeholder)
            .override(resize, resize)
            .centerCrop()
            .into(imageView)
    }

    override fun loadImage(
        context: Context,
        resizeX: Int,
        resizeY: Int,
        imageView: ImageView,
        uri: Uri
    ) {
        Glide.with(context)
            .load(uri)
            .override(resizeX, resizeY)
            .priority(Priority.HIGH)
            .fitCenter()
            .into(imageView)
    }

    override fun loadUrlImage(
        context: Context,
        resizeX: Int,
        resizeY: Int,
        imageView: ImageView,
        url: String
    ) {
        Glide.with(context)
            .load(url)
            .override(resizeX, resizeY)
            .priority(Priority.HIGH)
            .fitCenter()
            .into(imageView)
    }

    override fun loadUriImage(context: Context, imageView: ImageView, path: String) {
        Glide.with(context)
            .load(path)
            .priority(Priority.HIGH)
            .fitCenter()
            .into(imageView)
    }

    override fun loadDrawableImage(context: Context, imageView: ImageView, resourceId: Int) {
        Glide.with(context)
            .load(resourceId)
            .priority(Priority.HIGH)
            .fitCenter()
            .into(imageView)
    }

    override fun loadGifImage(
        context: Context,
        resizeX: Int,
        resizeY: Int,
        imageView: ImageView,
        uri: Uri
    ) {
        Glide.with(context)
            .load(uri)
            .asGif()
            .override(resizeX, resizeY)
            .priority(Priority.HIGH)
            .into(imageView)
    }

    override fun supportAnimatedGif(): Boolean {
        return true
    }

    override fun pauseRequests(context: Context) {
        if (!assertValidRequest(context)) {
            return
        }
        Glide.with(context).pauseRequests()
    }

    override fun resumeRequests(context: Context) {
        if (!assertValidRequest(context)) {
            return
        }
        Glide.with(context).resumeRequests()
    }
}