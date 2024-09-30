package com.furaibo.rfc3161timestamptoolweb.service;

import jakarta.annotation.PostConstruct;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import java.util.*;


@Service
public class PDFService {

    // メンバ変数
    private final DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("yyyyMMddhhmmss");
    private Path outputFileRootPath;
    private Path outputFileFolderPath;
    private Path tmpFileRootPath;

    @Value("${app.output.folder.file}")
    private String outputFileRootPathStr;

    @Value("${app.tmp.folder.file}")
    private String tmpFileRootPathStr;

    @Value("${app.jp.font.file}")
    private String jpFontPathStr;

    @PostConstruct
    public void init() {
        try {
            this.outputFileRootPath = Paths.get(outputFileRootPathStr);
            this.tmpFileRootPath = Paths.get(tmpFileRootPathStr);
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
     * @return Path
     */
    public Path attachFiles(
            Path baseFilePath, List<Path> attachFilePathList)
            throws IOException {

        // ディレクトリの作成
        Files.createDirectories(this.tmpFileRootPath);

        // 出力先ファイルパス
        Path outputFilePath = this.tmpFileRootPath.resolve(
                UUID.randomUUID().toString() + ".pdf");

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

        return outputFilePath;
    }

    /**
     * 添付ファイル埋め込み用のPDF作成
     *
     * @param title
     * @param description
     * @return Path
     */
    public Path makeBasePdfFile(String title, String description) throws IOException {

        // ディレクトリの作成
        Files.createDirectories(this.tmpFileRootPath);

        // 出力先ファイルパス
        Path outputFilePath = this.tmpFileRootPath.resolve(
                UUID.randomUUID().toString() + ".pdf");

        // 出力用の文字列リストを作成
        List<String> inputLines = List.of(
            "[Date]", LocalDateTime.now().toString(), "",
            "[Title]", title, "",
            "[Description]", description
        );

        // PDDocumentおよびPDPageの初期化
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        // フォント及びmediabox関連の変数初期化
        PDFont pdfFont = PDType0Font.load(document, new File(jpFontPathStr));
        float fontSize = 18;
        float leading = 1.5f * fontSize;
        float initMargin = 72;
        float boxMargin = leading;
        int lineCount = 0;

        // add text table to PDFへのテキスト追加処理
        try {
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                for (String text : inputLines) {
                    // mediaboxの初期化および開始位置の指定
                    PDRectangle mediabox = page.getMediaBox();
                    float width = mediabox.getWidth() - 2 * initMargin;
                    float startX = mediabox.getLowerLeftX() + initMargin;
                    float startY = mediabox.getUpperRightY() - initMargin - boxMargin * (lineCount + 1);

                    // 複数行の折り返し処理
                    List<String> lines = new ArrayList<String>();
                    int lastSpace = -1;
                    while (text.length() > 0) {
                        int spaceIndex = text.indexOf(' ', lastSpace + 1);
                        if (spaceIndex < 0) {
                            spaceIndex = text.length();
                        }
                        String subString = text.substring(0, spaceIndex);
                        float size = fontSize * pdfFont.getStringWidth(subString) / 1000;

                        if (size > width) {
                            if (lastSpace < 0) {
                                lastSpace = spaceIndex;
                            }
                            subString = text.substring(0, lastSpace);
                            lines.add(subString);
                            text = text.substring(lastSpace).trim();
                            lastSpace = -1;
                        } else if (spaceIndex == text.length()) {
                            lines.add(text);
                            text = "";
                        } else {
                            lastSpace = spaceIndex;
                        }
                    }

                    // PDFページへのテキスト追加
                    contentStream.beginText();
                    contentStream.setFont(pdfFont, fontSize);
                    contentStream.newLineAtOffset(startX, startY);
                    for (String line : lines) {
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -leading);
                    }
                    contentStream.endText();

                    // 行カウントの加算(madiabox位置調整用)
                    if (lines.size() == 0) {
                        lineCount++;
                    } else {
                        lineCount += lines.size();
                    }
                }
            }

            // save PDF
            document.save(outputFilePath.toString());
            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // return path
        return outputFilePath;
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
