package com.zhongjh.cameraapp.phone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.zhongjh.albumcamerarecorder.album.filter.BaseFilter;
import com.zhongjh.albumcamerarecorder.settings.AlbumSetting;
import com.zhongjh.albumcamerarecorder.settings.CameraSetting;
import com.zhongjh.albumcamerarecorder.settings.GlobalSetting;
import com.zhongjh.albumcamerarecorder.settings.MultiMediaSetting;
import com.zhongjh.albumcamerarecorder.settings.RecorderSetting;
import com.zhongjh.cameraapp.BaseActivity;
import com.zhongjh.cameraapp.R;
import com.zhongjh.cameraapp.configuration.GifSizeFilter;
import com.zhongjh.cameraapp.configuration.Glide4Engine;
import com.zhongjh.cameraapp.databinding.ActivityMainThemeBinding;
import com.zhongjh.common.entity.SaveStrategy;
import com.zhongjh.common.enums.MimeType;
import com.zhongjh.displaymedia.apapter.AudioAdapter;
import com.zhongjh.displaymedia.apapter.ImagesAndVideoAdapter;
import com.zhongjh.displaymedia.entity.DisplayMedia;
import com.zhongjh.displaymedia.listener.DisplayMediaLayoutListener;
import com.zhongjh.displaymedia.widget.DisplayMediaLayout;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 多种样式版
 *
 * @author zhongjh
 */
public class MainThemeActivity extends BaseActivity {

    ActivityMainThemeBinding mBinding;

    GlobalSetting mGlobalSetting;

    /**
     * @param activity 要跳转的activity
     */
    public static void newInstance(Activity activity) {
        activity.startActivity(new Intent(activity, MainThemeActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainThemeBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // 以下为点击事件
        mBinding.dmlImageList.setDisplayMediaLayoutListener(new DisplayMediaLayoutListener() {

            @Override
            public void onItemAudioStartDownload(@NonNull AudioAdapter.AudioHolder holder, @NonNull String url) {

            }

            @Override
            public void onAddDataSuccess(@NotNull List<DisplayMedia> displayMedia) {
                //                // 如果需要其他参数的话，循环数据初始化相关数值，这个读取时间会较长，建议异步线程执行
//                for (MultiMediaView item : multiMediaViews) {
//                    item.initDataByPath();
//                }
            }

            @Override
            public void onItemAdd(@NotNull View view, @NotNull DisplayMedia displayMedia, int alreadyImageCount, int alreadyVideoCount, int alreadyAudioCount) {
                // 点击添加
                boolean isOk = getPermissions(false);
                if (isOk) {
                    openMain(alreadyImageCount, alreadyVideoCount, alreadyAudioCount);
                }
            }

            @Override
            public void onItemClick(@NotNull View view, @NotNull DisplayMedia displayMedia) {
                // 点击详情
                if (displayMedia.isImageOrGif() || displayMedia.isVideo()) {
//                    mGlobalSetting.openPreviewData(MainThemeActivity.this, REQUEST_CODE_CHOOSE,
//                            mBinding.dmlImageList.getImagesAndVideos(),
//                            mBinding.dmlImageList.getImagesAndVideos().indexOf(multiMediaView));
                }
            }

            @Override
            public void onItemStartUploading(@NonNull DisplayMedia displayMedia, @NonNull ImagesAndVideoAdapter.PhotoViewHolder viewHolder) {
                // 开始模拟上传 - 指刚添加后的。这里可以使用你自己的上传事件
                MyTask timer = new MyTask(displayMedia);
                timers.put(displayMedia, timer);
                timer.schedule();
            }

            @Override
            public void onItemClose(@NotNull DisplayMedia displayMedia) {
                // 停止上传
                MyTask myTask = timers.get(displayMedia);
                if (myTask != null) {
                    myTask.cancel();
                    timers.remove(displayMedia);
                }
            }

            @Override
            public boolean onItemVideoStartDownload(@NotNull View view, @NotNull DisplayMedia displayMedia, int position) {
                return false;
            }

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGlobalSetting != null) {
            mGlobalSetting.onDestroy();
        }
    }

    @Override
    protected DisplayMediaLayout getMaskProgressLayout() {
        return mBinding.dmlImageList;
    }

    @Override
    protected void openMain(int alreadyImageCount, int alreadyVideoCount, int alreadyAudioCount) {
        // 拍摄有关设置
        CameraSetting cameraSetting = new CameraSetting();
        // 支持的类型：图片，视频
        cameraSetting.mimeTypeSet(MimeType.ofAll());
        // 相册
        AlbumSetting albumSetting = new AlbumSetting(true)
                // 支持的类型：图片，视频
                .mimeTypeSet(MimeType.ofAll())
                // 是否显示多选图片的数字
                .countable(true)
                // 自定义过滤器
                .addFilter(new GifSizeFilter(320, 320, 5 * BaseFilter.K * BaseFilter.K))
                // 开启原图
                .originalEnable(true)
                // 最大原图size,仅当originalEnable为true的时候才有效
                .maxOriginalSize(10);

        // 录音机
        RecorderSetting recorderSetting = new RecorderSetting();
        // 全局
        mGlobalSetting = MultiMediaSetting.from(MainThemeActivity.this).choose(MimeType.ofAll());

        // 样式选择
        if (mBinding.rbBlue.isChecked()) {
            mGlobalSetting.theme(R.style.AppTheme_Blue);
        }
        if (mBinding.rbBlack.isChecked()) {
            mGlobalSetting.theme(R.style.AppTheme_Dracula);
        }


        mGlobalSetting.albumSetting(albumSetting);
        mGlobalSetting.cameraSetting(cameraSetting);
        mGlobalSetting.recorderSetting(recorderSetting);
        mGlobalSetting
                // 设置路径和7.0保护路径等等
                .allStrategy(new SaveStrategy(true, "com.zhongjh.cameraapp.fileprovider", "AA/test"))
                // for glide-V4
                .imageEngine(new Glide4Engine())
                // 最大10张图片或者最大1个视频
                .maxSelectablePerMediaType(null,
                        5,
                        3,
                        3,
                        alreadyImageCount,
                        alreadyVideoCount,
                        alreadyAudioCount)
                .forResult(REQUEST_CODE_CHOOSE);
    }

}
