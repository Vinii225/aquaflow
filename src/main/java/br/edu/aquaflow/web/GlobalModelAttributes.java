package br.edu.aquaflow.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    @Value("${aquaflow.school.name}")
    private String schoolName;

    @Value("${aquaflow.school.address}")
    private String schoolAddress;

    @Value("${aquaflow.school.phone}")
    private String schoolPhone;

    @ModelAttribute("schoolName")
    public String schoolName() {
        return schoolName;
    }

    @ModelAttribute("schoolAddress")
    public String schoolAddress() {
        return schoolAddress;
    }

    @ModelAttribute("schoolPhone")
    public String schoolPhone() {
        return schoolPhone;
    }
}
