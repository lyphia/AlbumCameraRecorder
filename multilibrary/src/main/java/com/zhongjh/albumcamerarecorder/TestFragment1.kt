package com.zhongjh.albumcamerarecorder

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.zhongjh.albumcamerarecorder.album.ui.MatissFragment

class TestFragment1 : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.item_album_zjh, container, false)
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        Log.d("MainFragment", "onViewCreated")
//        // 先通过标签形式查找
//        val matissFragment = childFragmentManager.findFragmentByTag("1")
//        // 如果不存在，则重新创建并添加，如果已经存在就不用处理了，因为FragmentStateAdapter已经帮我们处理了
//        matissFragment ?: let {
//            val newMatissFragment = TestFragment1()
//            childFragmentManager.beginTransaction()
//                .add(R.id.fragmentContainerView, newMatissFragment,
//                    "1"
//                )
//                .commitAllowingStateLoss()
//        }
//    }
}