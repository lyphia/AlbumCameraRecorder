package com.zhongjh.albumcamerarecorder.camera.ui.camera.state.type;

import com.zhongjh.albumcamerarecorder.camera.ui.camera.BaseCameraFragment;
import com.zhongjh.albumcamerarecorder.camera.ui.camera.manager.CameraVideoManager;
import com.zhongjh.albumcamerarecorder.camera.ui.camera.state.CameraStateManager;
import com.zhongjh.albumcamerarecorder.camera.ui.camera.state.StateMode;
import com.zhongjh.albumcamerarecorder.camera.ui.camera.manager.CameraPictureManager;

/**
 * 正在录制视频中的状态
 *
 * @author zhongjh
 * @date 2021/11/25
 */
public class VideoIn extends StateMode {

    /**
     * @param cameraFragment     主要是多个状态围绕着cameraLayout进行相关处理
     * @param cameraStateManager 可以让状态更改别的状态
     */
    public VideoIn(BaseCameraFragment<? extends CameraStateManager,
            ? extends CameraPictureManager,
            ? extends CameraVideoManager> cameraFragment, CameraStateManager cameraStateManager) {
        super(cameraFragment, cameraStateManager);
    }

    @Override
    public String getName() {
        return "VideoIn";
    }

    @Override
    public Boolean onBackPressed() {
        // 如果是录制中则暂停视频
        getCameraFragment().getCameraVideoManager().setBreakOff(true);
        getCameraFragment().getCameraManage().closeVideo();
        // 重置按钮
        getCameraFragment().getPhotoVideoLayout().reset();
        // 恢复预览状态
        getCameraStateManagement().setState(getCameraStateManagement().getPreview());
        return true;
    }

    @Override
    public boolean onActivityResult(int resultCode) {
        return true;
    }

    @Override
    public void longClickShort(long time) {
        // 母窗体显示底部
        getCameraFragment().getMainActivity().showHideTableLayout(true);
    }

    @Override
    public void pauseRecord(boolean isShort) {
        if (isShort) {
            // 重置底部按钮
            getCameraFragment().getPhotoVideoLayout().reset();
            getCameraStateManagement().setState(getCameraStateManagement().getPreview());
        } else {
            // 设置视频完成状态
            getCameraStateManagement().setState(getCameraStateManagement().getVideoComplete());
        }
    }
}
