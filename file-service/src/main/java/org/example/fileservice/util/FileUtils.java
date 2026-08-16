package org.example.fileservice.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUtils {

    public static void copyInputStreamToFile(InputStream inputStream, File targetFile) throws IOException {
        if (targetFile.getParentFile() != null) {
            targetFile.getParentFile().mkdirs();
        }
        Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 上传图片文件
     * @param file 上传的文件
     * @param uploadPath 上传根路径（如 ./picture）
     * @param folder 子文件夹（如 avatar、equipment、course）
     * @return 文件的相对路径（如 /avatar/uuid.jpg）
     */
    public static String uploadImage(MultipartFile file, String uploadPath, String folder) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只能上传图片文件");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + extension;

        // 构建目标目录
        File folderFile = new File(uploadPath, folder);
        if (!folderFile.exists()) {
            folderFile.mkdirs();
        }

        File targetFile = new File(folderFile, newFilename).getAbsoluteFile();
        file.transferTo(targetFile);

        return "/" + folder + "/" + newFilename;
    }

    /**
     * 删除文件
     * @param relativePath 文件相对路径（如 /avatar/uuid.jpg）
     * @param uploadPath 上传根路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String relativePath, String uploadPath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }
        File file = new File(uploadPath + relativePath.replace("/", File.separator));
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
