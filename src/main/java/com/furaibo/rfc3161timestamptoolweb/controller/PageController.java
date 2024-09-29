package com.furaibo.rfc3161timestamptoolweb.controller;

import com.furaibo.rfc3161timestamptoolweb.model.ActionHistory;
import com.furaibo.rfc3161timestamptoolweb.model.Document;
import com.furaibo.rfc3161timestamptoolweb.repository.ActionHistoryRepository;
import com.furaibo.rfc3161timestamptoolweb.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


@Controller
public class PageController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ActionHistoryRepository actionHistoryRepository;

    @GetMapping({"/", "/uploader"})
    public String showUploader(Model model){
        model.addAttribute("message", "This is sample message.");
        return "uploader";
    }

    @GetMapping("/document")
    public String showDocuments(Model model){
        List<Document> documents = documentRepository.getLatestWithLimit(10);
        model.addAttribute("documents", documents);
        return "document";
    }

    @GetMapping("/history")
    public String showHistories(Model model){
        List<ActionHistory> histories = actionHistoryRepository.getLatestWithLimit(10);
        model.addAttribute("histories", histories);
        return "history";
    }

}
