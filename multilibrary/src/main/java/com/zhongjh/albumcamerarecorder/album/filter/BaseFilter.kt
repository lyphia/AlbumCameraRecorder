package com.zhongjh.albumcamerarecorder.album.filter

import android.content.Context
import com.zhongjh.common.entity.IncapableCause
import com.zhongjh.common.entity.LocalMedia
import com.zhongjh.common.enums.MimeType

/**
 * Filter for choosing . You can add multiple Filters through
 * @author zhongjh
 */
abstract class BaseFilter {
    /**
     * Against what mime types this filter applies.
     * 针对这个过滤器应用的mime类型。
     * @return MimeType
     */
    protected abstract fun constraintTypes(): Set<MimeType>

    /**
     * Invoked for filtering each item.
     *
     * 调用以过滤每个项。
     * @param context 上下文
     * @param item item
     * @return null if selectable, [IncapableCause] if not selectable.
     */
    abstract fun filter(context: Context, item: LocalMedia): IncapableCause?

    /**
     * Whether an [LocalMedia] need filtering.
     */
    protected fun needFiltering(item: LocalMedia): Boolean {
        for (type in constraintTypes()) {
            if (type.checkType(item.path)) {
                return true
            }
        }
        return false
    }


    companion object {

        /**
         * Convenient constant for 1024.
         */
        const val K = 1024
    }
}