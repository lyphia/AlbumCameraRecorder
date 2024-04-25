package com.zhongjh.common.coordinator

/**
 * 视频压缩协调者
 *
 * @author zhongjh
 */
interface VideoCompressCoordinator {

    /**
     * 压缩视频
     * @param oldPath      压缩前的文件地址
     * @param compressPath 压缩后的文件地址
     */
    fun compress(oldPath: String, compressPath: String)
}