package com.example.AIG_ForgeHub.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException exception,HttpServletRequest request,RedirectAttributes redirectAttributes) {
        log.error("Resource not found at {}: {}",request.getRequestURI(),exception.getMessage(),exception);
        redirectAttributes.addFlashAttribute("error",exception.getMessage());
        return "redirect:"+getBackUrl(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException exception,HttpServletRequest request,RedirectAttributes redirectAttributes) {
        log.warn("Invalid request at {}: {}",request.getRequestURI(),exception.getMessage());
        redirectAttributes.addFlashAttribute("error",exception.getMessage());
        return "redirect:"+getBackUrl(request);
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception exception,HttpServletRequest request,RedirectAttributes redirectAttributes) {
        log.error("Unexpected error at {}: {}",request.getRequestURI(),exception.getMessage(),exception);
        redirectAttributes.addFlashAttribute("error","Something went wrong. Please try again.");
        return "redirect:"+getBackUrl(request);
    }

    private String getBackUrl(HttpServletRequest request) {
        String referer=request.getHeader("Referer");
        if(referer!=null && !referer.isBlank()) {
            return referer.substring(referer.indexOf(request.getContextPath())+request.getContextPath().length());
        }
        return "/auth/login";
    }
}