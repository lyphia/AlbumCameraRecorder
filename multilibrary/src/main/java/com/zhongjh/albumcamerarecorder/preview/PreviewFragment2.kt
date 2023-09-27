package com.zhongjh.albumcamerarecorder.preview

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.zhongjh.albumcamerarecorder.MainModel
import com.zhongjh.albumcamerarecorder.album.ui.album.SelectedData
import com.zhongjh.albumcamerarecorder.preview.base.BasePreviewFragment2
import com.zhongjh.common.entity.LocalMedia

class PreviewFragment2 : BasePreviewFragment2() {

    /**
     * 来源于Activity的MainModel
     */
    private lateinit var mMainModel: MainModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.mMainModel = ViewModelProvider(requireActivity())[MainModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    /**
     * 获取当前的数据
     */
    override fun getDatas(): ArrayList<LocalMedia> {
        Log.d(
            "PreviewFragment2",
            " size:" + mMainModel.localMedias.size + " page:" + mMainModel.page
        )
        return mMainModel.localMedias
    }

    /**
     * 获取当前显示的文件索引
     */
    override fun getPreviewPosition(): Int {
        return mMainModel.previewPosition
    }

    /**
     * 设置当前显示的文件索引
     */
    override fun setPreviewPosition(previewPosition: Int) {
        mMainModel.previewPosition = previewPosition
    }

    /**
     * 返回当前所选择的数据操作类
     * @return 返回当前所选择的数据操作类
     */
    override fun getSelectedData() : SelectedData {
        return mMainModel.selectedData
    }
}