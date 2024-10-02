package com.furaibo.rfc3161timestamptoolweb.service;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileService {

    // メンバ変数
    @Value("${app.upload.folder.file}")
    private String uploadFileRootPathStr;

    @Value("${app.output.folder.file}")
    private String outputFileRootPathStr;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    /**
     * Multipart形式のファイルのアップロード処理
     *
     * @param file
     * @return
     */
    public Path saveUploadFile(
            MultipartFile file,
            String fileNamePreFix,
            boolean useRandomFileName) throws IOException {

        // 時刻とフォルダ名の取得
        LocalDateTime ldt = LocalDateTime.now();
        String folderName = ldt.format(dtf);

        // フォルダパスの取得
        Path uploadFileRootPath = Paths.get(uploadFileRootPathStr);
        Path fileFolderPath = uploadFileRootPath.resolve(folderName);

        // フォルダ作成
        Files.createDirectories(fileFolderPath);

        // ファイルパスの取得
        Path filePath;
        if (useRandomFileName) {
            // 拡張子の取得およびランダムなファイル名の設定
            String ext = FilenameUtils.getExtension(file.getOriginalFilename());
            filePath = this.getRandomSaveFilePath(fileFolderPath, fileNamePreFix, ext);
        } else {
            filePath = fileFolderPath.resolve(fileNamePreFix + file.getOriginalFilename());
        }

        // ファイルの保存処理
        try (OutputStream os = Files.newOutputStream(filePath, StandardOpenOption.CREATE)) {
            byte[] bytes = file.getBytes();
            os.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return filePath;
    }

    private Path getRandomSaveFilePath(
            Path folderPath, String fileNamePrefix, String ext) throws IOException {
        // UUIDによるファイル名の決定
        String filename = UUID.randomUUID().toString();
        return folderPath.resolve(fileNamePrefix + filename + "." + ext);
    }

}
