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

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Multipart形式のファイルのアップロード処理
     *
     * @param file
     * @return
     */
    public Path saveUploadFile(MultipartFile file) throws IOException {
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());

        // フォルダの作成
        Path uploadFileRootPath = Paths.get(uploadFileRootPathStr);
        Path fileFolderPath = this.getFileFolderPath(uploadFileRootPath);
        Path filePath = this.getSaveFilePath(fileFolderPath, ext);

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

    public Path copyFileToOutputFolder(Path inputFilePath) throws IOException {
        String ext = FilenameUtils.getExtension(inputFilePath.toString());

        // フォルダの作成
        Path outputFileRootPath = Paths.get(outputFileRootPathStr);
        Path outputFileFolderPath = this.getFileFolderPath(outputFileRootPath);
        Path outputFilePath = this.getSaveFilePath(outputFileFolderPath, ext);

        // ファイルコピーの処理
        Files.copy(inputFilePath, outputFilePath);

        return outputFilePath;
    }

    private Path getSaveFilePath(Path folderPath, String ext) throws IOException {
        // UUIDによるファイル名の決定
        String filename = UUID.randomUUID().toString();
        return folderPath.resolve(filename + "." + ext);
    }

    private Path getFileFolderPath(Path rootFolderPath) throws IOException {
        // 日付によるサブフォルダ名の決定
        LocalDateTime ldt = LocalDateTime.now();
        String folderName = ldt.format(dtf);

        // フォルダの作成
        Path fileFolderPath = rootFolderPath.resolve(folderName);
        if (!Files.exists(fileFolderPath)) {
            Files.createDirectories(fileFolderPath);
        }

        return fileFolderPath;
    }

}
