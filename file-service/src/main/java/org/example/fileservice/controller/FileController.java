package org.example.fileservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.fileservice.common.Result;
import org.example.fileservice.util.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@CrossOrigin(origins = "*")
public class FileController {

    @Value("${file.upload-path:./picture}")
    private String uploadPath;

    @PostMapping("/upload")
    @SentinelResource(value = "file-upload", blockHandler = "handleBlock")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "folder", defaultValue = "common") String folder) {
        try {
            String relativePath = FileUtils.uploadImage(file, uploadPath, folder);
            Map<String, String> data = Map.of("url", relativePath);
            return Result.success("上传成功", data);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (IOException e) {
            return Result.error(500, "文件保存失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    @SentinelResource(value = "file-delete", blockHandler = "handleBlock")
    public Result<Void> delete(@RequestParam("path") String filePath) {
        boolean deleted = FileUtils.deleteFile(filePath, uploadPath);
        if (deleted) {
            return Result.success("删除成功", null);
        }
        return Result.error(404, "文件不存在");
    }

    public Result<Void> handleBlock(BlockException e) {
        return Result.error(429, "请求过于频繁，请稍后再试");
    }
}
