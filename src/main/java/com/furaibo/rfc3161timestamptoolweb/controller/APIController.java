package com.furaibo.rfc3161timestamptoolweb.controller;

import com.furaibo.rfc3161timestamptoolweb.model.ActionHistory;
import com.furaibo.rfc3161timestamptoolweb.model.Document;
import com.furaibo.rfc3161timestamptoolweb.repository.ActionHistoryRepository;
import com.furaibo.rfc3161timestamptoolweb.repository.DocumentRepository;
import com.furaibo.rfc3161timestamptoolweb.service.FileService;
import com.furaibo.rfc3161timestamptoolweb.service.PDFService;
import com.furaibo.rfc3161timestamptoolweb.service.TimeStampService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Controller
@RestController
@RequestMapping("/api/")
public class APIController {

    private Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    private TimeStampService tss;
    private PDFService pdfs;
    private FileService fls;

    @Autowired
    public APIController(TimeStampService tss, PDFService pdfs, FileService fls) {
        this.tss = tss;
        this.pdfs = pdfs;
        this.fls = fls;
    }

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ActionHistoryRepository actionHistoryRepository;

    @GetMapping("/test")
    public String testTimeStampResponse(){
        try {
            byte[] inputBytes = DigestUtils.sha256("Hello, World!".getBytes());
            tss.testTimeStampResponse(inputBytes);
        } catch(Exception e) {
            e.printStackTrace();
            return "failed!";
        }

        return "It works!";
    }

    @PostMapping("/document/{documentID}/update/title")
    public void updateDocumentTitle(
            @PathVariable("documentID") Integer documentID,
            @RequestParam(name="title") String title,
            HttpServletResponse response) throws IOException {

        // 値の更新
        Document doc = documentRepository.getReferenceById(documentID);
        doc.setTitle(title);
        doc.renewUpdatedAt();
        documentRepository.save(doc);

        // 操作履歴の追加
        ActionHistory hist = new ActionHistory();
        hist.setActionTitle("ドキュメント情報(タイトル)の更新");
        hist.setActionDesc("documentID: " + documentID);
        actionHistoryRepository.save(hist);

        // リダイレクト
        response.sendRedirect("/document/" + documentID + "?mode=updateDocument");
    }

    @PostMapping("/document/{documentID}/update/description")
    public void updateDocumentDescription(
            @PathVariable("documentID") Integer documentID,
            @RequestParam(name="description") String description,
            HttpServletResponse response) throws IOException {

        // 値の更新
        Document doc = documentRepository.getReferenceById(documentID);
        doc.setDescription(description);
        doc.renewUpdatedAt();
        documentRepository.save(doc);

        // 操作履歴の追加
        ActionHistory hist = new ActionHistory();
        hist.setActionTitle("ドキュメント情報(内容説明)の更新");
        hist.setActionDesc("documentID: " + documentID);
        actionHistoryRepository.save(hist);

        // リダイレクト
        response.sendRedirect("/document/" + documentID + "?mode=updateDocument");
    }

    @PostMapping("/document/{documentID}/update/note")
    public void updateDocumentNote(
            @PathVariable("documentID") Integer documentID,
            @RequestParam(name="note") String note,
            HttpServletResponse response) throws IOException {

        // 値の更新
        Document doc = documentRepository.getReferenceById(documentID);
        doc.setDescription(note);
        doc.renewUpdatedAt();
        documentRepository.save(doc);

        // 操作履歴の追加
        ActionHistory hist = new ActionHistory();
        hist.setActionTitle("ドキュメント情報(補足事項)の更新");
        hist.setActionDesc("documentID: " + documentID);
        actionHistoryRepository.save(hist);

        // リダイレクト
        response.sendRedirect("/document/" + documentID + "?mode=updateDocument");
    }

    @PostMapping("/add/timestamp/pdf")
    public void addTimeStampToPdf(
            @RequestParam("uploadFiles") List<MultipartFile> files,
            @RequestParam(name="title", required=false) String title,
            @RequestParam(name="description", required=false) String description,
            @RequestParam(name="note", required=false) String note,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) throws IOException {

        try {
            for (MultipartFile file : files) {
                // ファイル名の取得
                String fileName = file.getOriginalFilename();

                // ファイルのアップロード処理
                Path uploadFilePath = fls.saveUploadFile(file, "", true);

                // タイムスタンプの付加
                Path timeStampFilePath = tss.addTimeStampToSingleFile(uploadFilePath);

                // ドキュメント管理レコードの追加
                Document doc = new Document();
                doc.setUploadFilePath(uploadFilePath.toString());
                doc.setTimestampFilePath(timeStampFilePath.toString());
                doc.setVerifiedAt(LocalDateTime.now());

                // タイトル及び説明文の追加
                if (files.size() == 1) {
                    doc.setTitle(title);
                    doc.setDescription(description);
                    doc.setNote(note);
                } else {
                    doc.setTitle(fileName);
                }

                // レコードの記録
                documentRepository.save(doc);

                // 操作履歴の追加
                ActionHistory hist = new ActionHistory();
                hist.setActionTitle("新規PDFファイルの追加");
                hist.setActionDesc("PDFファイルへのタイムスタンプ追加 - ファイル名: " + fileName);
                actionHistoryRepository.save(hist);
            }

            // リダイレクト先への属性値の設定
            redirectAttributes.addFlashAttribute("addDocument", "1");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // リダイレクト
        response.sendRedirect("/document" + "?mode=addDocument");
    }

    @PostMapping("/add/timestamp/nonpdf")
    public void addTimeStampToNonPdf(
        @RequestParam("uploadFiles") List<MultipartFile> files,
        @RequestParam(name="title", required=false) String title,
        @RequestParam(name="description", required=false) String description,
        @RequestParam(name="note", required=false) String note,
        HttpServletResponse response) throws IOException {

        try {
            // アップロードファイルパス格納用のリスト
            List<Path> uploadFilePathList = new ArrayList<>();

            // ファイルのアップロード処理
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String fileNamePrefix = String.format("%02d", i);
                Path uploadFilePath = fls.saveUploadFile(file, fileNamePrefix, false);
                uploadFilePathList.add(uploadFilePath);
            }

            // 埋め込み元PDFファイルの作成
            Path basePdfFilePath = pdfs.makeBasePdfFile(title, description);

            // ファイルの埋め込み処理
            Path attachedFilePath = pdfs.attachFiles(basePdfFilePath, uploadFilePathList);

            // タイムスタンプの付加
            Path timeStampFilePath = tss.addTimeStampToSingleFile(attachedFilePath);

            // ドキュメント管理レコードの追加
            Document doc = new Document();
            doc.setUploadFilePath(attachedFilePath.toString());
            doc.setTimestampFilePath(timeStampFilePath.toString());
            doc.setVerifiedAt(LocalDateTime.now());

            // タイトル及び説明文の追加、レコードの記録
            doc.setTitle(title);
            doc.setDescription(description);
            doc.setNote(note);
            documentRepository.save(doc);

            // 操作履歴の追加
            ActionHistory hist = new ActionHistory();
            hist.setActionTitle("PDF形式以外のファイル追加");
            hist.setActionDesc("非PDFファイルのPDF形式変換およびタイムスタンプ追加");
            actionHistoryRepository.save(hist);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // リダイレクト
        response.sendRedirect("/document" + "?mode=addDocument");
    }

    @GetMapping("/document/download")
    public ResponseEntity<Resource> downloadDocumentFile(
            @RequestParam("key") String downloadKey) throws IOException {

        // ダウンロードキーによるドキュメント検索
        Document doc = documentRepository.getByDownloadKey(downloadKey);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        // タイムスタンプ付与済みファイルおよびファイル名の準備
        Path filePath = Paths.get(doc.getTimestampFilePath());
        Resource resource = new PathResource(filePath);
        String downloadFileName = doc.getTitle() + "." + FilenameUtils.getExtension(resource.getFilename());

        // ファイルダウンロード用のレスポンス返却
        return ResponseEntity.ok()
                .contentType(getContentType(filePath))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadFileName + "\"")
                .body(resource);
    }

    /*
    @GetMapping("/document/verify")
    public ResponseEntity<Resource> verifyDocumentFiles(
            @RequestParam("documentIds") List<Integer> ids) throws IOException, TSPException, CMSException {

        // IDのリストによるドキュメント検索
        List<Document> documentList = documentRepository.findByIDs(ids);

        // 入力ファイルパスのリストの準備
        List<Path> inputFilePathList = new ArrayList<Path>();
        for (Document doc : documentList) {
            Path filePath = Paths.get(doc.getTimestampFilePath());
            inputFilePathList.add(filePath);
        }

        // 検証結果CSVファイルの準備
        Path outputCsvFilePath = tss.verifyTimeStampInMultipleFiles(inputFilePathList);

        // 前回検証日時の更新処理
        LocalDateTime dtNow = LocalDateTime.now();
        for (Document doc : documentList) {
            doc.setVerifiedAt(dtNow);
            documentRepository.save(doc);
        }

        // タイムスタンプ付与済みファイルおよびファイル名の準備
        Resource resource = new PathResource(outputCsvFilePath);
        String downloadCsvFileName = FilenameUtils.getName(outputCsvFilePath.toString());

        // ファイルダウンロード用のレスポンス返却
        return ResponseEntity.ok()
                .contentType(getContentType(outputCsvFilePath))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadCsvFileName + "\"")
                .body(resource);
    }
     */

    private MediaType getContentType(Path path) throws IOException {
        try {
            return MediaType.parseMediaType(Files.probeContentType(path));
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
