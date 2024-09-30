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
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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
    public String getDocumentsList(){
        List<Document> docs = documentRepository.getLatestWithLimit(10);
        return gson.toJson(docs);
    }

    @GetMapping("/histories/list")
    public String getHistoriesList() {
        List<ActionHistory> docs = actionHistoryRepository.getLatestWithLimit(10);
        return gson.toJson(docs);
    }

    @PostMapping("/add/timestamp/pdf")
    public String addTimeStampToPdf(
            @RequestParam("uploadFiles") List<MultipartFile> files,
            @RequestParam(name="title", required=false) String title,
            @RequestParam(name="description", required=false) String description) {

        try {
            for (MultipartFile file : files) {
                // ファイル名の取得
                String fileName = file.getOriginalFilename();

                // ファイルのアップロード処理
                Path uploadFilePath = fls.saveUploadFile(file);

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

        } catch (Exception e) {
            e.printStackTrace();
            return "failed!";
        }

        return "It works!";
    }

    @PostMapping("/add/timestamp/nonpdf")
    public String addTimeStampToNonPdf(
        @RequestParam("uploadFiles") List<MultipartFile> files,
        @RequestParam(name="title", required=false) String title,
        @RequestParam(name="description", required=false) String description) {

        try {
            // アップロードファイルパス格納用のリスト
            List<Path> uploadFilePathList = new ArrayList<>();

            // ファイルのアップロード処理
            for (MultipartFile file : files) {
                Path uploadFilePath = fls.saveUploadFile(file);
                uploadFilePathList.add(uploadFilePath);
            }

            // 埋め込み元PDFファイルの作成
            Path basePdfFilePath = pdfs.makeBasePdfFile(title, description);

            // ファイルの埋め込み処理
            Path attachedFilePath = pdfs.attachFiles(basePdfFilePath, uploadFilePathList);

            // タイムスタンプの付加
            tss.addTimeStampToSingleFile(attachedFilePath);

        } catch (Exception e) {
            e.printStackTrace();
            return "failed!";
        }

        return "It works!";
    }

    /*
    @GetMapping("/histories/")
    public String verifyTimeStamp() {
        // TODO
    }
     */
}
