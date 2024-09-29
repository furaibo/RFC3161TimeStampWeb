package com.furaibo.rfc3161timestamptoolweb.service;

import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PageMode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


@Service
public class PDFService {

    // メンバ変数
    @Value("${app.output.folder.file}")
    private String outputFileRootPathStr;

    private final DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("yyyyMMddhhmmss");
    private Path outputFileRootPath;
    private Path outputFileFolderPath;


    @PostConstruct
    public void init() {
        try {
            this.outputFileRootPath = Paths.get(outputFileRootPathStr);
            this.outputFileFolderPath = this.outputFileRootPath.resolve(dtFormat.format(LocalDateTime.now()));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * PDFファイルのマージ処理
     *
     * @param inputFilePathList
     * @throws IOException
     */
    public void mergeFiles(List<Path> inputFilePathList) throws IOException {

        // ディレクトリの作成
        Files.createDirectories(this.outputFileFolderPath);

        // 出力先ファイルパス
        String outputFilePathStr = String.format(
                "%s/%s.pdf",
                this.outputFileFolderPath, dtFormat.format(LocalDateTime.now())
            );
        Path outputFilePath = Paths.get(outputFilePathStr);

        // PDFMergeUtilityの初期化
        PDFMergerUtility pdfMerger = new PDFMergerUtility();

        // 対象ファイルの追加
        for (Path inputFilePath : inputFilePathList) {
            pdfMerger.addSource(inputFilePath.toString());
        }

        // PDFのマージ処理実行
        FileOutputStream fos = new FileOutputStream(outputFilePath.toString());
        pdfMerger.setDestinationStream(fos);
        pdfMerger.mergeDocuments(null);
    }

    /**
     * 指定のPDFファイルへのファイル添付処理
     *
     * @param baseFilePath
     * @param attachFilePathList
     * @throws IOException
     */
    public void attachFiles(
            Path baseFilePath, List<Path> attachFilePathList)
            throws IOException {

        // ディレクトリの作成
        Files.createDirectories(this.outputFileFolderPath);

        // 出力先ファイルパス
        String outputFilePathStr = String.format(
                "%s/%s.pdf",
                this.outputFileFolderPath, dtFormat.format(LocalDateTime.now())
            );
        Path outputFilePath = Paths.get(outputFilePathStr);

        // DDocumentおよびツリーノードの初期化
        PDDocument document = Loader.loadPDF(baseFilePath.toFile());
        PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();
        HashMap embeddedFileMap = new HashMap();

        // Add attachment files
        for (Path path: attachFilePathList) {
            // ファイル名とサブタイプの取得
            String fileName = path.getFileName().toString();
            String subtypeStr = this.getSubtypeFromFileExtension(fileName);

            // 入力ストリームの設定
            File inputFile = new File(path.toString());
            byte[] bytes = FileUtils.readFileToByteArray(inputFile);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

            // 添付ファイルの基本設定
            PDEmbeddedFile ef = new PDEmbeddedFile(document, bis);
            ef.setSubtype(subtypeStr);
            ef.setSize((int)inputFile.length());
            ef.setCreationDate(Calendar.getInstance());

            // 添付ファイルの詳細設定
            PDComplexFileSpecification pcfs = new PDComplexFileSpecification();
            pcfs.setFile(fileName);
            pcfs.setFileUnicode(fileName);
            pcfs.setFileDescription("Attachment File for RFC-3161 TimeStamp");
            pcfs.setEmbeddedFile(ef);
            pcfs.setEmbeddedFileUnicode(ef);

            // 添付ファイル情報マップの更新
            embeddedFileMap.put(fileName, pcfs);
        }

        // 添付ファイル情報マップをツリーに設定
        efTree.setNames(embeddedFileMap);
        efTree.getCOSObject().removeItem(COSName.LIMITS);

        // PDFカタログの設定
        PDDocumentNameDictionary names = new PDDocumentNameDictionary(document.getDocumentCatalog());
        names.setEmbeddedFiles(efTree);
        document.getDocumentCatalog().setNames(names);
        efTree.getCOSObject().removeItem(COSName.LIMITS);

        // PDFファイル閲覧時に添付ファイルタブを表示するよう設定
        document.getDocumentCatalog().setPageMode(PageMode.USE_ATTACHMENTS);

        // ファイル保存
        document.save(outputFilePath.toString());
        document.close();
    }

    /**
     * PDFへの添付ファイル設定時のサブタイプ文字列取得
     *
     * @param fileName
     * @return
     */
    private String getSubtypeFromFileExtension(String fileName) {
        String subtypeStr;
        String fileExt = fileName.split("\\.")[1].toLowerCase(Locale.ROOT);

        subtypeStr = switch (fileExt) {
            case "txt" -> "text/plain";
            case "pdf" -> "application/pdf";
            case "jpeg", "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "zip" -> "application/zip";
            case "7z" -> "application/x-7z-compressed";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            default -> "";
        };

        return subtypeStr;
    }

}
