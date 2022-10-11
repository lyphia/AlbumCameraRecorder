package com.zhongjh.cameraapp.phone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;

import com.zhongjh.albumcamerarecorder.AlbumCameraRecorderApi;
import com.zhongjh.albumcamerarecorder.album.filter.BaseFilter;
import com.zhongjh.albumcamerarecorder.camera.constants.FlashModels;
import com.zhongjh.albumcamerarecorder.camera.entity.BitmapData;
import com.zhongjh.albumcamerarecorder.camera.listener.OnCaptureListener;
import com.zhongjh.albumcamerarecorder.listener.OnResultCallbackListener;
import com.zhongjh.albumcamerarecorder.settings.AlbumSetting;
import com.zhongjh.albumcamerarecorder.settings.CameraSetting;
import com.zhongjh.albumcamerarecorder.settings.GlobalSetting;
import com.zhongjh.albumcamerarecorder.settings.MultiMediaSetting;
import com.zhongjh.albumcamerarecorder.settings.RecorderSetting;
import com.zhongjh.cameraapp.BaseActivity;
import com.zhongjh.cameraapp.R;
import com.zhongjh.cameraapp.configuration.GifSizeFilter;
import com.zhongjh.cameraapp.configuration.Glide4Engine;
import com.zhongjh.cameraapp.configuration.ImageCompressionLuBan;
import com.zhongjh.cameraapp.databinding.ActivityMainBinding;
import com.zhongjh.common.entity.LocalFile;
import com.zhongjh.common.entity.MediaExtraInfo;
import com.zhongjh.common.entity.MultiMedia;
import com.zhongjh.common.entity.SaveStrategy;
import com.zhongjh.common.enums.MimeType;
import com.zhongjh.common.utils.MediaUtils;
import com.zhongjh.common.utils.UriUtils;
import com.zhongjh.progresslibrary.entity.MultiMediaView;
import com.zhongjh.progresslibrary.listener.MaskProgressLayoutListener;
import com.zhongjh.progresslibrary.widget.MaskProgressLayout;
import com.zhongjh.videoedit.VideoCompressManager;
import com.zhongjh.videoedit.VideoMergeManager;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * 配置版
 *
 * @author zhongjh
 */
public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivityTEST";

    ActivityMainBinding mBinding;

    GlobalSetting mGlobalSetting;
    AlbumSetting mAlbumSetting;

    @GlobalSetting.ScreenOrientation
    int requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    @FlashModels
    int flashModel = FlashModels.TYPE_FLASH_OFF;

    /**
     * @param activity 要跳转的activity
     */
    public static void newInstance(Activity activity) {
        activity.startActivity(new Intent(activity, MainActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mBinding.llScreenOrientation.setOnClickListener(v -> showPopupMenu());

        mBinding.llFlashModel.setOnClickListener(v -> showFlashPopupMenu());

        // 设置九宫格的最大呈现数据
        mBinding.mplImageList.setMaxMediaCount(getMaxCount(), getImageCount(), getVideoCount(), getAudioCount());

        // 以下为点击事件
        mBinding.mplImageList.setMaskProgressLayoutListener(new MaskProgressLayoutListener() {

            @Override
            public void onAddDataSuccess(@NotNull List<MultiMediaView> multiMediaViews) {
            }

            @Override
            public void onItemAdd(@NotNull View view, @NotNull MultiMediaView multiMediaView, int alreadyImageCount, int alreadyVideoCount, int alreadyAudioCount) {
                openMain(alreadyImageCount, alreadyVideoCount, alreadyAudioCount);
            }

            @Override
            public void onItemClick(@NotNull View view, @NotNull MultiMediaView multiMediaView) {
                // 点击详情
                if (multiMediaView.isImageOrGif() || multiMediaView.isVideo()) {
                    mGlobalSetting.openPreviewData(MainActivity.this, REQUEST_CODE_CHOOSE,
                            mBinding.mplImageList.getImagesAndVideos(),
                            mBinding.mplImageList.getImagesAndVideos().indexOf(multiMediaView));
                }
            }

            @Override
            public void onItemStartUploading(@NotNull MultiMediaView multiMediaView) {
                // 开始模拟上传 - 指刚添加后的。这里可以使用你自己的上传事件
                MyTask timer = new MyTask(multiMediaView);
                timers.put(multiMediaView, timer);
                timer.schedule();
            }

            @Override
            public void onItemClose(@NotNull View view, @NotNull MultiMediaView multiMediaView) {
                // 停止上传
                MyTask myTask = timers.get(multiMediaView);
                if (myTask != null) {
                    myTask.cancel();
                    timers.remove(multiMediaView);
                }
            }

            @Override
            public void onItemAudioStartDownload(@NotNull View view, @NotNull String url) {

            }

            @Override
            public boolean onItemVideoStartDownload(@NotNull View view, @NotNull MultiMediaView multiMediaView) {
                return false;
            }

        });

        // 获取文件大小 文件目录：context.getExternalCacheDir()
        mBinding.btnFileSize.setOnClickListener(v -> mBinding.tvFileSize.setText(AlbumCameraRecorderApi.getFileSize(getApplication())));

        // 删除文件缓存 文件目录：context.getExternalCacheDir()
        mBinding.btnDeleteFileCache.setOnClickListener(v -> AlbumCameraRecorderApi.deleteCacheDirFile(getApplication()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGlobalSetting != null) {
            mGlobalSetting.onDestroy();
        }
    }

    @Override
    protected MaskProgressLayout getMaskProgressLayout() {
        return mBinding.mplImageList;
    }

    /**
     * @param alreadyImageCount 已经存在显示的几张图片
     * @param alreadyVideoCount 已经存在显示的几个视频
     * @param alreadyAudioCount 已经存在显示的几个音频
     *                          打开窗体
     */
    @Override
    protected void openMain(int alreadyImageCount, int alreadyVideoCount, int alreadyAudioCount) {
        if (!check()) {
            return;
        }

        // 刷新九宫格的最大呈现数据
        mBinding.mplImageList.setMaxMediaCount(getMaxCount(), getImageCount(), getVideoCount(), getAudioCount());

        // 拍摄有关设置
        CameraSetting cameraSetting = initCameraSetting();

        // 相册设置
        mAlbumSetting = initAlbumSetting();

        // 录音机设置
        RecorderSetting recorderSetting = new RecorderSetting();

        //  全局
        Set<MimeType> mimeTypes = MimeType.ofAll();
        if (mBinding.rbAllAll.isChecked()) {
            mimeTypes = MimeType.ofAll();
        } else if (mBinding.rbAllVideo.isChecked()) {
            mimeTypes = MimeType.ofVideo();
        } else if (mBinding.rbAllImage.isChecked()) {
            mimeTypes = MimeType.ofImage();
        }

        mGlobalSetting = MultiMediaSetting.from(MainActivity.this).choose(mimeTypes);
        // 默认从第二个开始
        mGlobalSetting.defaultPosition(1);
        // 启动过场动画，从下往上动画
        mGlobalSetting.isCutscenes(mBinding.cbIsCutscenes.isChecked());
        // 是否支持编辑图片，预览相册、拍照处拥有编辑功能
        mGlobalSetting.isImageEdit(mBinding.cbIsEdit.isChecked());
        if (mBinding.cbAlbum.isChecked())
        // 开启相册功能
        {
            mGlobalSetting.albumSetting(mAlbumSetting);
        }
        if (mBinding.cbCamera.isChecked())
        // 开启拍摄功能
        {
            mGlobalSetting.cameraSetting(cameraSetting);
        }
        if (mBinding.cbRecorder.isChecked())
        // 开启录音功能
        {
            mGlobalSetting.recorderSetting(recorderSetting);
        }

        // 设置横竖屏
        mGlobalSetting.setRequestedOrientation(requestedOrientation);

        // 是否压缩图片
        if (mBinding.cbIsCompressImage.isChecked()) {
            mGlobalSetting.setOnImageCompressionInterface(new ImageCompressionLuBan());
        }

        // 是否压缩视频
        if (mBinding.cbIsCompressVideo.isChecked()) {
            mGlobalSetting.videoCompress(new VideoCompressManager());
        }

        // 自定义路径，如果其他子权限设置了路径，那么以子权限为准
        if (!TextUtils.isEmpty(mBinding.etAllFile.getText().toString())) {
            // 设置路径和7.0保护路径等等，只影响录制拍照的路径，选择路径还是按照当前选择的路径
            mGlobalSetting.allStrategy(
                    new SaveStrategy(true, "com.zhongjh.cameraapp.fileprovider", mBinding.etAllFile.getText().toString()));
        }
        if (!TextUtils.isEmpty(mBinding.etPictureFile.getText().toString())) {
            // 设置路径和7.0保护路径等等，只影响录制拍照的路径，选择路径还是按照当前选择的路径
            mGlobalSetting.pictureStrategy(
                    new SaveStrategy(true, "com.zhongjh.cameraapp.fileprovider", mBinding.etPictureFile.getText().toString()));
        }
        if (!TextUtils.isEmpty(mBinding.etAudioFile.getText().toString())) {
            // 设置路径和7.0保护路径等等，只影响录制拍照的路径，选择路径还是按照当前选择的路径
            mGlobalSetting.audioStrategy(
                    new SaveStrategy(true, "com.zhongjh.cameraapp.fileprovider", mBinding.etAudioFile.getText().toString()));
        }
        if (!TextUtils.isEmpty(mBinding.etVideoFile.getText().toString())) {
            // 设置路径和7.0保护路径等等，只影响录制拍照的路径，选择路径还是按照当前选择的路径
            mGlobalSetting.videoStrategy(
                    new SaveStrategy(true, "com.zhongjh.cameraapp.fileprovider", mBinding.etVideoFile.getText().toString()));
        }

        // 加载图片框架，具体注释看maxSelectablePerMediaType方法注释
        mGlobalSetting.imageEngine(new Glide4Engine())
                .maxSelectablePerMediaType(
                        getMaxCount(),
                        getImageCount(),
                        getVideoCount(),
                        getAudioCount(),
                        alreadyImageCount,
                        alreadyVideoCount,
                        alreadyAudioCount);
        if (mBinding.cbIsActivityResult.isChecked()) {
            mGlobalSetting.forResult(REQUEST_CODE_CHOOSE);
        } else {
            initForResult();
        }
    }

    private void initForResult() {
        mGlobalSetting.forResult(new OnResultCallbackListener() {

            @Override
            public void onResult(@NotNull List<? extends LocalFile> result) {
                for (LocalFile localFile : result) {
                    Log.i(TAG, "onResult id:" + localFile.getId());
                    Log.d(TAG, "onResult 绝对路径:" + localFile.getPath());
                    Log.d(TAG, "onResult 旧图路径:" + localFile.getOldPath());
                    Log.d(TAG, "onResult 原图路径:" + localFile.getOriginalPath());
                    Log.d(TAG, "onResult Uri:" + localFile.getUri());
                    Log.d(TAG, "onResult 旧图Uri:" + localFile.getOldUri());
                    Log.d(TAG, "onResult 原图Uri:" + localFile.getOriginalUri());
                    Log.d(TAG, "onResult 文件大小: " + localFile.getSize());
                    Log.d(TAG, "onResult 视频音频长度: " + localFile.getDuration());
                    Log.i(TAG, "onResult 是否选择了原图: " + localFile.isOriginal());
                    if (localFile.isImageOrGif()) {
                        if (localFile.isImage()) {
                            Log.d(TAG, "onResult 图片类型");
                        } else if (localFile.isImage()) {
                            Log.d(TAG, "onResult 图片类型");
                        }
                    } else if (localFile.isVideo()) {
                        Log.d(TAG, "onResult 视频类型");
                    } else if (localFile.isAudio()) {
                        Log.d(TAG, "onResult 音频类型");
                    }
                    Log.d(TAG, "onResult 具体类型:" + localFile.getMimeType());
                    // 某些手机拍摄没有自带宽高，那么我们可以自己获取
                    if (localFile.getWidth() == 0 && localFile.isVideo()) {
                        if (localFile.getPath() != null) {
                            MediaExtraInfo mediaExtraInfo = MediaUtils.getVideoSize(getApplication(), localFile.getPath());
                            localFile.setWidth(mediaExtraInfo.getWidth());
                            localFile.setHeight(mediaExtraInfo.getHeight());
                            localFile.setDuration(mediaExtraInfo.getDuration());
                        }
                    }
                    Log.d(TAG, "onResult 宽高: " + localFile.getWidth() + "x" + localFile.getHeight());
                    Log.d(TAG, UriUtils.uriToFile(getApplicationContext(), localFile.getUri()).getPath());
                }
                getMaskProgressLayout().addLocalFileStartUpload(result);
            }

            @Override
            public void onResultFromPreview(@NotNull List<? extends MultiMedia> result, boolean apply) {
                if (apply) {
                    for (MultiMedia multiMedia : result) {
                        // 绝对路径,AndroidQ如果存在不属于自己App下面的文件夹则无效
                        Log.i(TAG, "onResult id:" + multiMedia.getId());
                        Log.i(TAG, "onResult 绝对路径:" + multiMedia.getPath());
                        Log.d(TAG, "onResult 旧图路径:" + multiMedia.getOldPath());
                        Log.d(TAG, "onResult 原图路径:" + multiMedia.getOriginalPath());
                        Log.i(TAG, "onResult Uri:" + multiMedia.getUri());
                        Log.d(TAG, "onResult 旧图Uri:" + multiMedia.getOldUri());
                        Log.d(TAG, "onResult 原图Uri:" + multiMedia.getOriginalUri());
                        Log.i(TAG, "onResult 文件大小: " + multiMedia.getSize());
                        Log.i(TAG, "onResult 视频音频长度: " + multiMedia.getDuration());
                        Log.i(TAG, "onResult 是否选择了原图: " + multiMedia.isOriginal());
                        if (multiMedia.isImageOrGif()) {
                            if (multiMedia.isImage()) {
                                Log.d(TAG, "onResult 图片类型");
                            } else if (multiMedia.isImage()) {
                                Log.d(TAG, "onResult 图片类型");
                            }
                        } else if (multiMedia.isVideo()) {
                            Log.d(TAG, "onResult 视频类型");
                        } else if (multiMedia.isAudio()) {
                            Log.d(TAG, "onResult 音频类型");
                        }
                        Log.i(TAG, "onResult 具体类型:" + multiMedia.getMimeType());
                        Log.i(TAG, "onResult 宽高: " + multiMedia.getWidth() + "x" + multiMedia.getHeight());
                    }
                    // 倒数循环判断，如果不存在，则删除
                    for (int i = getMaskProgressLayout().getImagesAndVideos().size() - 1; i >= 0; i--) {
                        int k = 0;
                        for (LocalFile localFile : result) {
                            if (!getMaskProgressLayout().getImagesAndVideos().get(i).equals(localFile)) {
                                k++;
                            }
                        }
                        if (k == result.size()) {
                            // 所有都不符合，则删除
                            getMaskProgressLayout().removePosition(i);
                        }
                    }
                }
            }
        });
    }

    /**
     * @return 拍摄设置
     */
    private CameraSetting initCameraSetting() {
        CameraSetting cameraSetting = new CameraSetting();
        Set<MimeType> mimeTypeCameras;
        if (mBinding.cbCameraImage.isChecked() && mBinding.cbCameraVideo.isChecked()) {
            mimeTypeCameras = MimeType.ofAll();
            // 支持的类型：图片，视频
            cameraSetting.mimeTypeSet(mimeTypeCameras);
        } else if (mBinding.cbCameraImage.isChecked()) {
            mimeTypeCameras = MimeType.ofImage();
            // 支持的类型：图片
            cameraSetting.mimeTypeSet(mimeTypeCameras);
        } else if (mBinding.cbCameraVideo.isChecked()) {
            mimeTypeCameras = MimeType.ofVideo();
            // 支持的类型：视频
            cameraSetting.mimeTypeSet(mimeTypeCameras);
        }
        // 最长录制时间
        cameraSetting.duration(Integer.parseInt(mBinding.etCameraDuration.getText().toString()));
        // 最短录制时间限制，单位为毫秒，即是如果长按在1500毫秒内，都暂时不开启录制
        cameraSetting.minDuration(Integer.parseInt(mBinding.etMinCameraDuration.getText().toString()));
        // 是否启用水印
        if (mBinding.cbWatermark.isChecked()) {
            cameraSetting.watermarkResource(R.layout.watermark);
        }

        if (mBinding.cbVideoMerge.isChecked()) {
            // 启动这个即可开启视频分段录制合并功能
            cameraSetting.videoMerge(new VideoMergeManager());
        }

        // 是否启用闪光灯记忆模式
        cameraSetting.enableFlashMemoryModel(mBinding.cbFlashMemoryModel.isChecked());

        // 闪光灯默认模式
        cameraSetting.flashModel(flashModel);

        // 开启点击即开启录制(失去点击拍照功能)
        cameraSetting.isClickRecord(mBinding.cbClickRecord.isChecked());

        // 开启高清拍照(失去录像功能)
        cameraSetting.enableImageHighDefinition(mBinding.cbPictureHD.isChecked());

        // 开启高清录像(失去拍照功能)
        cameraSetting.enableVideoHighDefinition(mBinding.cbVideoHD.isChecked());

        // 拍照时添加图片事件以及删除图片事件
        cameraSetting.setOnCaptureListener(new OnCaptureListener() {
            @Override
            public void remove(@NonNull List<? extends BitmapData> captureData, int position) {
                Log.d(TAG, "删除索引 " + position);
            }

            @Override
            public void add(@NonNull List<? extends BitmapData> captureDatas, int position) {
                Log.d(TAG, "添加索引 " + position);
            }
        });

        return cameraSetting;
    }

    /**
     * @return 相册设置
     */
    private AlbumSetting initAlbumSetting() {
        AlbumSetting albumSetting = new AlbumSetting(!mBinding.cbMediaTypeExclusive.isChecked());
        Set<MimeType> mimeTypeAlbum;
        if (mBinding.cbAlbumImage.isChecked() && mBinding.cbAlbumVideo.isChecked()) {
            mimeTypeAlbum = MimeType.ofAll();
            // 支持的类型：图片，视频
            albumSetting.mimeTypeSet(mimeTypeAlbum);
        } else if (mBinding.cbAlbumImage.isChecked()) {
            mimeTypeAlbum = MimeType.ofImage();
            // 支持的类型：图片，视频
            albumSetting.mimeTypeSet(mimeTypeAlbum);
        } else if (mBinding.cbAlbumVideo.isChecked()) {
            mimeTypeAlbum = MimeType.ofVideo();
            // 支持的类型：图片，视频
            albumSetting.mimeTypeSet(mimeTypeAlbum);
        }

        albumSetting
                // 如果选择的媒体只有图像或视频，是否只显示一种媒体类型
                .showSingleMediaType(mBinding.cbShowSingleMediaTypeTrue.isChecked())
                // 是否显示多选图片的数字
                .countable(mBinding.cbCountableTrue.isChecked())
                // 自定义过滤器
                .addFilter(new GifSizeFilter(Integer.parseInt(mBinding.etAddFilterMinWidth.getText().toString()), Integer.parseInt(mBinding.etAddFilterMinHeight.getText().toString()), Integer.parseInt(mBinding.etMaxSizeInBytes.getText().toString()) * BaseFilter.K * BaseFilter.K))
                // 九宫格大小 ,建议这样使用getResources().getDimensionPixelSize(R.dimen.grid_expected_size)
                .gridExpectedSize(dip2px(Integer.parseInt(mBinding.etGridExpectedSize.getText().toString())))
                // 图片缩放比例
                .thumbnailScale(0.85f)
                .setOnSelectedListener(localFiles -> {
                    // 每次选择的事件
                    Log.d("onSelected", "onSelected: localFiles.size()=" + localFiles.size());
                })
                // 开启原图
                .originalEnable(mBinding.cbOriginalEnableTrue.isChecked())
                // 是否启动相册列表滑动隐藏顶部和底部控件，上滑隐藏、下滑显示
                .slidingHiddenEnable(mBinding.cbSlideHideEnable.isChecked())
                // 最大原图size,仅当originalEnable为true的时候才有效
                .maxOriginalSize(Integer.parseInt(mBinding.etMaxOriginalSize.getText().toString()))
                .setOnCheckedListener(isChecked -> {
                    // 是否勾选了原图
                    Log.d("isChecked", "onCheck: isChecked=" + isChecked);
                });
        return albumSetting;
    }

    /**
     * @return 返回 图片、视频、音频能选择的上限
     */
    private Integer getMaxCount() {
        if (!mBinding.etMaxCount.getText().toString().isEmpty()) {
            return Integer.parseInt(mBinding.etMaxCount.getText().toString());
        }
        return null;
    }

    /**
     * @return 返回图片能选择的上限
     */
    private Integer getImageCount() {
        if (!mBinding.etAlbumCount.getText().toString().isEmpty()) {
            return Integer.parseInt(mBinding.etAlbumCount.getText().toString());
        }
        return null;
    }

    /**
     * @return 返回视频能选择的上限
     */
    private Integer getVideoCount() {
        if (!mBinding.etVideoCount.getText().toString().isEmpty()) {
            return Integer.parseInt(mBinding.etVideoCount.getText().toString());
        }
        return null;
    }

    /**
     * @return 返回音频能选择的上限
     */
    private Integer getAudioCount() {
        if (!mBinding.etAudioCount.getText().toString().isEmpty()) {
            return Integer.parseInt(mBinding.etAudioCount.getText().toString());
        }
        return null;
    }

    /**
     * 检测正确性
     *
     * @return 是否正确
     */
    private boolean check() {
        if (getMaxCount() == null && getImageCount() == null) {
            Toast.makeText(getApplicationContext(), "maxSelectablePerMediaType 方法中如果 maxSelectable 为null，那么 maxImageSelectable 必须是0或者0以上数值",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getMaxCount() == null && getVideoCount() == null) {
            Toast.makeText(getApplicationContext(), "maxSelectablePerMediaType 方法中如果 maxSelectable 为null，那么 maxVideoSelectable 必须是0或者0以上数值",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getMaxCount() == null && getAudioCount() == null) {
            Toast.makeText(getApplicationContext(), "maxSelectablePerMediaType 方法中如果 maxSelectable 为null，那么 maxAudioSelectable 必须是0或者0以上数值",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getMaxCount() != null && getImageCount() != null && getImageCount() > getMaxCount()) {
            Toast.makeText(getApplicationContext(), "maxSelectable 必须比 maxImageSelectable 大",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getMaxCount() != null && getVideoCount() != null && getVideoCount() > getMaxCount()) {
            Toast.makeText(getApplicationContext(), "maxSelectable 必须比 maxVideoSelectable 大",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        if (getMaxCount() != null && getAudioCount() != null && getAudioCount() > getMaxCount()) {
            Toast.makeText(getApplicationContext(), "maxSelectable 必须比 maxAudioSelectable 大",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    private void showPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(this, mBinding.llScreenOrientation);
        popupMenu.inflate(R.menu.menu_screenorientation);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.actionUnspecified:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_UNSPECIFIED");
                    break;
                case R.id.actionLandscape:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_LANDSCAPE");
                    break;
                case R.id.actionPortrait:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_PORTRAIT");
                    break;
                case R.id.actionUser:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_USER");
                    break;
                case R.id.actionBehind:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_BEHIND");
                    break;
                case R.id.actionSensor:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_SENSOR");
                    break;
                case R.id.actionNosensor:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_NOSENSOR");
                    break;
                case R.id.actionSensorLandscape:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_SENSOR_LANDSCAPE");
                    break;
                case R.id.actionReverseLandscape:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_REVERSE_LANDSCAPE");
                    break;
                case R.id.actionReversePortrait:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_REVERSE_PORTRAIT");
                    break;
                case R.id.actionFullSensor:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_FULL_SENSOR");
                    break;
                case R.id.actionUserLandscape:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_USER_LANDSCAPE");
                    break;
                case R.id.actionUserPortrait:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_USER_PORTRAIT");
                    break;
                case R.id.actionFullUser:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_FULL_USER");
                    break;
                case R.id.actionLocked:
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
                    mBinding.tvScreenOrientation.setText("SCREEN_ORIENTATION_LOCKED");
                    break;
                default:
                    //do nothing
            }

            return false;
        });
        popupMenu.show();
    }

    /**
     * 弹窗闪光灯选项
     */
    @SuppressLint("NonConstantResourceId")
    private void showFlashPopupMenu() {
        PopupMenu popupMenu = new PopupMenu(this, mBinding.llFlashModel);
        popupMenu.inflate(R.menu.menu_flash);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            switch (menuItem.getItemId()) {
                case R.id.actionFlashOff:
                    flashModel = FlashModels.TYPE_FLASH_OFF;
                    mBinding.tvFlashModel.setText(getResources().getString(R.string.flash_off));
                    break;
                case R.id.actionFlashOn:
                    flashModel = FlashModels.TYPE_FLASH_ON;
                    mBinding.tvFlashModel.setText(getResources().getString(R.string.flash_on));
                    break;
                case R.id.actionFlashAuto:
                    flashModel = FlashModels.TYPE_FLASH_AUTO;
                    mBinding.tvFlashModel.setText(getResources().getString(R.string.flash_auto));
                    break;
                default:
                    //do nothing
            }

            return false;
        });
        popupMenu.show();
    }

}
