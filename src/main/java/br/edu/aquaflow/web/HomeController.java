package br.edu.aquaflow.web;

import br.edu.aquaflow.service.site.HomePageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final HomePageService homePageService;

    public HomeController(HomePageService homePageService) {
        this.homePageService = homePageService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("page", homePageService.load());
        return "home";
    }
}
