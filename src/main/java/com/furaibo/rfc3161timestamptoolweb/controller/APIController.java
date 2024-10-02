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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
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

    @GetMapping("/documents/list")
    @ResponseBody
    public String getDocumentsList(){
        List<Document> docs = documentRepository.getLatestWithLimit(10);
        return gson.toJson(docs);
    }

    @GetMapping("/histories/list")
    @ResponseBody
    public String getHistoriesList() {
        List<ActionHistory> docs = actionHistoryRepository.getLatestWithLimit(10);
        return gson.toJson(docs);
    }

    @PostMapping("/add/timestamp/pdf")
    public void addTimeStampToPdf(
            @RequestParam("uploadFiles") List<MultipartFile> files,
            @RequestParam(name="title", required=false) String title,
            @RequestParam(name="description", required=false) String description,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) throws IOException {

        try {
            for (MultipartFile file : files) {
                // ファイル名の取得
                String fileName = file.getOriginalFilename();

                // ファイルのアップロード処理
                Path uploadFilePath = fls.saveUploadFile(file, "", true);

                // タイムスタンプの付加
                tss.addTimeStampToSingleFile(uploadFilePath);

                // ドキュメント管理レコードの追加
                Document doc = new Document();
                doc.setUploadFilePath(uploadFilePath.toString());
                doc.setTimestampFilePath(uploadFilePath.toString());
                doc.setVerifiedAt(new Date());

                // タイトル及び説明文の追加
                if (files.size() == 1) {
                    doc.setTitle(title);
                    doc.setDescription(description);
                } else {
                    doc.setTitle(fileName);
                }

                // レコードの記録
                documentRepository.save(doc);

                // 操作履歴の追加
                ActionHistory hist = new ActionHistory();
                hist.setActionTitle("Add new pdf file");
                hist.setActionDesc("upload file and add timestamp - " + fileName);
                actionHistoryRepository.save(hist);
            }

            // リダイレクト先への属性値の設定
            redirectAttributes.addFlashAttribute("result", "success");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("result", "failed");
            e.printStackTrace();
        }

        // リダイレクト
        response.sendRedirect("/document");
    }

    @PostMapping("/add/timestamp/nonpdf")
    public void addTimeStampToNonPdf(
        @RequestParam("uploadFiles") List<MultipartFile> files,
        @RequestParam(name="title", required=false) String title,
        @RequestParam(name="description", required=false) String description,
        HttpServletResponse response,
        RedirectAttributes redirectAttributes) throws IOException {

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
            Path uploadFilePath = tss.addTimeStampToSingleFile(attachedFilePath);

            // ドキュメント管理レコードの追加
            Document doc = new Document();
            doc.setUploadFilePath(uploadFilePath.toString());
            doc.setTimestampFilePath(uploadFilePath.toString());
            doc.setVerifiedAt(new Date());

            // タイトル及び説明文の追加、レコードの記録
            doc.setTitle(title);
            doc.setDescription(description);
            documentRepository.save(doc);

            // 操作履歴の追加
            ActionHistory hist = new ActionHistory();
            hist.setActionTitle("Add non-pdf files");
            hist.setActionDesc("upload non pdf-files and add timestamp");
            actionHistoryRepository.save(hist);

            // リダイレクト先への属性値の設定
            redirectAttributes.addFlashAttribute("result", "success");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("result", "failed");
            e.printStackTrace();
        }

        // リダイレクト
        response.sendRedirect("/document");
    }

    @GetMapping("/document/download")
    public ResponseEntity<Resource> downloadDocumentFile(
            @RequestParam("key") String downloadKey) throws IOException {

        System.out.println("!!!");
        System.out.println(downloadKey);

        Document doc = documentRepository.getByDownloadKey(downloadKey);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(doc.getUploadFilePath());
        Resource resource = new PathResource(filePath);
        String downloadFileName = doc.getTitle() + "." +
                FilenameUtils.getExtension(resource.getFilename());

        return ResponseEntity.ok()
                .contentType(getContentType(filePath))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadFileName + "\"")
                .body(resource);
    }

    /*
    @GetMapping("/verify/timestamp/")
    public String verifyTimeStamp() {
        //
        // TODO
        //
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
