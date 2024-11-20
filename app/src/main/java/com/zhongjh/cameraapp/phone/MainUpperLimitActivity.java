package com.zhongjh.cameraapp.phone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zhongjh.albumcamerarecorder.album.filter.BaseFilter;
import com.zhongjh.albumcamerarecorder.settings.AlbumSetting;
import com.zhongjh.albumcamerarecorder.settings.CameraSetting;
import com.zhongjh.albumcamerarecorder.settings.GlobalSetting;
import com.zhongjh.albumcamerarecorder.settings.MultiMediaSetting;
import com.zhongjh.albumcamerarecorder.settings.RecorderSetting;
import com.zhongjh.cameraapp.BaseActivity;
import com.zhongjh.cameraapp.configuration.GifSizeFilter;
import com.zhongjh.cameraapp.configuration.Glide4Engine;
import com.zhongjh.cameraapp.databinding.ActivityMainUpperLimitBinding;
import com.zhongjh.cameraapp.model.LimitModel;
import com.zhongjh.common.entity.SaveStrategy;
import com.zhongjh.common.enums.MimeType;
import com.zhongjh.displaymedia.apapter.AudioAdapter;
import com.zhongjh.displaymedia.apapter.ImagesAndVideoAdapter;
import com.zhongjh.displaymedia.entity.DisplayMedia;
import com.zhongjh.displaymedia.listener.DisplayMediaLayoutListener;
import com.zhongjh.displaymedia.widget.DisplayMediaLayout;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author zhongjh
 * @date 2021/7/16
 */
public class MainUpperLimitActivity extends BaseActivity {

    private final String TAG = MainUpperLimitActivity.this.getClass().getSimpleName();

    ActivityMainUpperLimitBinding mBinding;
    GlobalSetting mGlobalSetting;


    /**
     * @param activity 要跳转的activity
     */
    public static void newInstance(Activity activity) {
        activity.startActivity(new Intent(activity, MainUpperLimitActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainUpperLimitBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mBinding.tvMessage.append("1. 这个配置十分灵活，总有一种适合你使用，可以配置 总共选择上限、图片选择上限、视频选择上限、音频选择上限。");
        mBinding.tvMessage.append("\n");
        mBinding.tvMessage.append("2. 如果图片、视频、音频不需要选择填0即可。");
        mBinding.tvMessage.append("\n");
        mBinding.tvMessage.append("3. 也支持各自没有上限，但是需要设置一个总共选择上限，最终可选择的数量以该值为准。");
        mBinding.tvMessage.append("\n");
        mBinding.tvMessage.append("4. 如果图片视频音频有各自的选择上限，那么以他们的上限为准。");
        mBinding.tvMessage.append("\n");
        mBinding.tvMessage.append("5. 如果其中一个例如图片为null，那么图片可选择无限，但是受限于总上限。");

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
//                    mGlobalSetting.openPreviewData(MainUpperLimitActivity.this, REQUEST_CODE_CHOOSE,
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

        // 清空重置
        mBinding.btnReset.setOnClickListener(v -> {
            mBinding.dmlImageList.reset();
            // 停止所有的上传
            for (Map.Entry<DisplayMedia, MyTask> entry : timers.entrySet()) {
                entry.getValue().cancel();
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
        mGlobalSetting = MultiMediaSetting.from(MainUpperLimitActivity.this).choose(MimeType.ofAll());
        // 开启相册功能
        mGlobalSetting.albumSetting(albumSetting);
        // 开启拍摄功能
        mGlobalSetting.cameraSetting(cameraSetting);
        // 开启录音功能
        mGlobalSetting.recorderSetting(recorderSetting);


        // 最大5张图片、最大3个视频、最大1个音频
        LimitModel limitModel = getLimitModel();
        mBinding.dmlImageList.setMaxMediaCount(limitModel.getMaxSelectable(),
                limitModel.getMaxImageSelectable(),
                limitModel.getMaxVideoSelectable(),
                limitModel.getMaxAudioSelectable());

        mGlobalSetting
                // 设置路径和7.0保护路径等等
                .allStrategy(new SaveStrategy(true, "com.zhongjh.cameraapp.fileprovider", "aabb"))
                // for glide-V4
                .imageEngine(new Glide4Engine())
                .maxSelectablePerMediaType(limitModel.getMaxSelectable(),
                        limitModel.getMaxImageSelectable(),
                        limitModel.getMaxVideoSelectable(),
                        limitModel.getMaxAudioSelectable(),
                        alreadyImageCount,
                        alreadyVideoCount,
                        alreadyAudioCount)
                .forResult(REQUEST_CODE_CHOOSE);
    }

    private LimitModel getLimitModel() {
        LimitModel limitModel = new LimitModel();
        // 根据选择类型返回相关上限设置
        if (mBinding.rbEachLimit.isChecked()) {
            limitModel.setMaxSelectable(null);
            limitModel.setMaxImageSelectable(2);
            limitModel.setMaxVideoSelectable(1);
            limitModel.setMaxAudioSelectable(1);
        } else if (mBinding.rbSumLimit.isChecked()) {
            limitModel.setMaxSelectable(20);
            limitModel.setMaxImageSelectable(null);
            limitModel.setMaxVideoSelectable(null);
            limitModel.setMaxAudioSelectable(null);
        } else if (mBinding.rbImageLimit.isChecked()) {
            limitModel.setMaxSelectable(5);
            limitModel.setMaxImageSelectable(null);
            limitModel.setMaxVideoSelectable(1);
            limitModel.setMaxAudioSelectable(1);
        } else if (mBinding.rbVideoLimit.isChecked()) {
            limitModel.setMaxSelectable(5);
            limitModel.setMaxImageSelectable(3);
            limitModel.setMaxVideoSelectable(null);
            limitModel.setMaxAudioSelectable(1);
        } else if (mBinding.rbAudioLimit.isChecked()) {
            limitModel.setMaxSelectable(5);
            limitModel.setMaxImageSelectable(3);
            limitModel.setMaxVideoSelectable(1);
            limitModel.setMaxAudioSelectable(null);
        } else if (mBinding.rbOne.isChecked()) {
            limitModel.setMaxSelectable(5);
            limitModel.setMaxImageSelectable(3);
            limitModel.setMaxVideoSelectable(null);
            limitModel.setMaxAudioSelectable(null);
        } else if (mBinding.rbTwo.isChecked()) {
            limitModel.setMaxSelectable(5);
            limitModel.setMaxImageSelectable(null);
            limitModel.setMaxVideoSelectable(2);
            limitModel.setMaxAudioSelectable(null);
        } else if (mBinding.rbThree.isChecked()) {
            limitModel.setMaxSelectable(5);
            limitModel.setMaxImageSelectable(null);
            limitModel.setMaxVideoSelectable(null);
            limitModel.setMaxAudioSelectable(2);
        }
        return limitModel;
    }

}
