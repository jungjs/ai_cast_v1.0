package com.aicast.controller;

import com.aicast.domain.gov.GovListRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    private final GovListRepository govListRepository;

    public WebController(GovListRepository govListRepository) {
        this.govListRepository = govListRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/stats")
    public String stats(Model model) {
        model.addAttribute("govList", govListRepository.findByIsActiveTrue());
        return "stats";
    }

    @GetMapping("/playground")
    public String playground(Model model) {
        model.addAttribute("govList", govListRepository.findByIsActiveTrue());
        return "playground";
    }
}
