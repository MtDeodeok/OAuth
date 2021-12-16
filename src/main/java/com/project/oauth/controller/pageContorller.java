package com.project.oauth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class pageContorller {
	
	@GetMapping("/")
	public void index() {
		
	}
	
	@GetMapping("login")
	public void login() {
		
	}
}
