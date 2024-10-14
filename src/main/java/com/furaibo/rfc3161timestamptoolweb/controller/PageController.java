package com.furaibo.rfc3161timestamptoolweb.controller;

import com.furaibo.rfc3161timestamptoolweb.model.ActionHistory;
import com.furaibo.rfc3161timestamptoolweb.model.Document;
import com.furaibo.rfc3161timestamptoolweb.repository.ActionHistoryRepository;
import com.furaibo.rfc3161timestamptoolweb.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Controller
public class PageController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ActionHistoryRepository actionHistoryRepository;

    @Value("${app.default.search.limit}")
    private int defaultSearchLimit;

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @GetMapping({"/", "/uploader"})
    public String showUploader(Model model){
        model.addAttribute("message", "This is sample message.");
        return "uploader";
    }

    @GetMapping("/document")
    public String showDocuments(
            @ModelAttribute("keyword") String keyword,
            @ModelAttribute("startDate") String startDate,
            @ModelAttribute("endDate") String endDate,
            @ModelAttribute("mode") String mode,
            Model model) throws ParseException {

        // 日付関連の処理
        LocalDate dtFrom, dtTo;
        LocalDate dtNow = LocalDate.now();
        if (startDate.isBlank()) {
            dtFrom = dtNow.minusYears(5);
        } else {
            dtFrom = LocalDate.parse(startDate, dtf);
        }
        if (endDate.isBlank()) {
            dtTo = dtNow;
        } else {
            dtTo = LocalDate.parse(endDate, dtf);
        }

        // ドキュメント情報をセット
        List<Document> documents;
        if (keyword.isBlank()) {
            if (startDate.isBlank() && endDate.isBlank()) {
                // 新しい順に検索
                documents = documentRepository.findLatestWithLimit(defaultSearchLimit);
            } else {
                // 日時のみで検索
                documents = documentRepository.findByDateRange(dtFrom, dtTo);
            }
        } else {
            if (startDate.isBlank() && endDate.isBlank()) {
                // キーワードのみで検索
                documents = documentRepository.findByKeyword(keyword);
            } else {
                // キーワード及び日時で検索
                documents = documentRepository.findByKeywordAndDateRange(keyword, dtFrom, dtTo);
            }
        }
        model.addAttribute("documents", documents);

        // メッセージ表示フラグ情報の設定
        if (mode.equals("addDocument")) {
            model.addAttribute("showAddDocument", true);
        } else if (mode.equals("updateDocument")) {
            model.addAttribute("showUpdateDocument", true);
        }

        return "document";
    }

    @GetMapping("/document/{documentID}")
    public String showDocuments(
            @PathVariable("documentID") Integer documentID,
            Model model) {

        // ドキュメント情報をセット
        Document document = documentRepository.getReferenceById(documentID);
        model.addAttribute("document", document);

        return "document_detail";
    }

    @GetMapping("/history")
    public String showHistories(Model model){
        // 操作履歴情報をセット
        List<ActionHistory> histories = actionHistoryRepository.findLatestWithLimit(defaultSearchLimit);
        model.addAttribute("histories", histories);
        return "history";
    }

}
