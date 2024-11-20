package com.zhongjh.cameraapp.phone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;

import com.zhongjh.albumcamerarecorder.album.filter.BaseFilter;
import com.zhongjh.albumcamerarecorder.settings.AlbumSetting;
import com.zhongjh.albumcamerarecorder.settings.CameraSetting;
import com.zhongjh.albumcamerarecorder.settings.GlobalSetting;
import com.zhongjh.albumcamerarecorder.settings.MultiMediaSetting;
import com.zhongjh.albumcamerarecorder.settings.RecorderSetting;
import com.zhongjh.cameraapp.BaseActivity;
import com.zhongjh.cameraapp.configuration.GifSizeFilter;
import com.zhongjh.cameraapp.configuration.Glide4Engine;
import com.zhongjh.cameraapp.databinding.ActivityMainSimpleBinding;
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
 * 简单版
 *
 * @author zhongjh
 */
public class MainSimpleActivity extends BaseActivity {

    ActivityMainSimpleBinding mBinding;
    private final String TAG = MainSimpleActivity.this.getClass().getSimpleName();
    private final Integer MAX_SELECTABLE = null;
    private final Integer MAX_IMAGE_SELECTABLE = 5;
    private final Integer MAX_VIDEO_SELECTABLE = 3;
    private final Integer MAX_AUDIO_SELECTABLE = 5;

    GlobalSetting mGlobalSetting;

    /**
     * @param activity 要跳转的activity
     */
    public static void newInstance(Activity activity) {
        activity.startActivity(new Intent(activity, MainSimpleActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainSimpleBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.dmlImageList.setMaxMediaCount(MAX_SELECTABLE, MAX_IMAGE_SELECTABLE, MAX_VIDEO_SELECTABLE, MAX_AUDIO_SELECTABLE);

        // 以下为点击事件
        mBinding.dmlImageList.setDisplayMediaLayoutListener(new DisplayMediaLayoutListener() {

            @Override
            public void onItemAudioStartDownload(@NonNull AudioAdapter.VideoHolder holder, @NonNull String url) {

            }

            @Override
            public void onItemAudioStartUploading(@NonNull DisplayMedia displayMedia, @NonNull AudioAdapter.VideoHolder viewHolder) {
                // 开始模拟上传 - 指刚添加后的。这里可以使用你自己的上传事件
                MyTask timer = new MyTask(displayMedia);
                timers.put(displayMedia, timer);
                timer.schedule();
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
//                    mGlobalSetting.openPreviewData(MainSimpleActivity.this, REQUEST_CODE_CHOOSE,
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
            public void onItemAudioStartDownload(@NotNull View view, @NotNull String url) {

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
        AlbumSetting albumSetting = new AlbumSetting(false)
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
        mGlobalSetting = MultiMediaSetting.from(MainSimpleActivity.this).choose(MimeType.ofAll());

        if (mBinding.cbAlbum.isChecked()) {
            // 开启相册功能
            mGlobalSetting.albumSetting(albumSetting);
        }
        if (mBinding.cbCamera.isChecked()) {
            // 开启拍摄功能
            mGlobalSetting.cameraSetting(cameraSetting);
        }
        if (mBinding.cbRecorder.isChecked()) {
            // 开启录音功能
            mGlobalSetting.recorderSetting(recorderSetting);
        }

        mGlobalSetting
                // 设置路径和7.0保护路径等等
                .allStrategy(new SaveStrategy(true, "com.zhongjh.cameraapp.fileprovider", "aabb"))
                // for glide-V4
                .imageEngine(new Glide4Engine())
                // 最大5张图片、最大3个视频、最大1个音频。如果需要使用九宫格，请把九宫格MaskProgressLayout的maxCount也改动 mBinding.dmlImageList.setMaxMediaCount();
                .maxSelectablePerMediaType(MAX_SELECTABLE, MAX_IMAGE_SELECTABLE, MAX_VIDEO_SELECTABLE, MAX_AUDIO_SELECTABLE,
                        alreadyImageCount,
                        alreadyVideoCount,
                        alreadyAudioCount)
                .forResult(REQUEST_CODE_CHOOSE);
    }

}
