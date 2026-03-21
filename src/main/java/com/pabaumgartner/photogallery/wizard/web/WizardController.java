package com.pabaumgartner.photogallery.wizard.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WizardController {

	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("step", 1);
		return "wizard/step1";
	}

	@GetMapping("/wizard/step1")
	public String step1(Model model) {
		model.addAttribute("step", 1);
		return "wizard/step1";
	}

	@PostMapping("/wizard/step1")
	public String step1Submit(@RequestParam String galleryName, @RequestParam(required = false) String galleryDescription, Model model) {
		model.addAttribute("galleryName", galleryName);
		model.addAttribute("galleryDescription", galleryDescription);
		model.addAttribute("step", 2);
		return "wizard/step2";
	}

	@GetMapping("/wizard/step2")
	public String step2(Model model) {
		model.addAttribute("step", 2);
		return "wizard/step2";
	}

	@PostMapping("/wizard/step2")
	public String step2Submit(@RequestParam String galleryName, @RequestParam(required = false) String galleryDescription, @RequestParam(required = false) String outputPath, Model model) {
		model.addAttribute("galleryName", galleryName);
		model.addAttribute("galleryDescription", galleryDescription);
		model.addAttribute("outputPath", outputPath != null && !outputPath.isBlank() ? outputPath : "gallery-output");
		model.addAttribute("step", 3);
		return "wizard/summary";
	}

}
