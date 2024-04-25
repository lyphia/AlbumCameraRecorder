package com.zhongjh.videoedit

import com.coremedia.iso.boxes.Container
import com.googlecode.mp4parser.FileDataSourceImpl
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack
import com.zhongjh.common.coordinator.VideoCompressCoordinator
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.util.*

/**
 * 视频压缩管理
 *
 * @author zhongjh
 * @date 2022/1/27
 */
class VideoCompressManager : VideoCompressCoordinator {

    @Throws(IOException::class)
    override fun compress(oldPath: String, compressPath: String) {
        // 创建压缩后的视频文件
        val outputFile = File(compressPath)
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }
        // 设置压缩参数
        val movie = MovieCreator.build(FileDataSourceImpl(oldPath))
        val tracks = movie.tracks
        movie.tracks = LinkedList()
        // 开始压缩视频
        val compressedMovie = Movie()
        for (track in tracks) {
            if (track.handler.equals("vide")) {
                compressedMovie.addTrack(CroppedTrack(track, 0, track.duration))
            }
        }
        val out: Container = DefaultMp4Builder().build(compressedMovie)
        val fc: FileChannel = RandomAccessFile(outputFile, "rw").channel
        out.writeContainer(fc)
        fc.close()
    }

}