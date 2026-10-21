package com.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.project.service.OffreService;

@Controller 
public class jobOffreController {
    private final OffreService offreService;

    public jobOffreController(OffreService offreService) {
        this.offreService = offreService;
    }

    @GetMapping("/jobOffre")
    public String getJobOffre(Model model) {
        model.addAttribute("offres", offreService.listerOffresPubliees());
        return "portail/listJob";
    }
}
