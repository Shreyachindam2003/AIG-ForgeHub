package com.example.AIG_ForgeHub.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        log.warn(
                "Resource not found | method={} path={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage()
        );

        return buildResponse(
                exception.getMessage(),
                HttpStatus.NOT_FOUND,
                request,
                model,
                redirectAttributes
        );
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(
            BusinessException exception,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        log.warn(
                "Business exception | method={} path={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage()
        );

        return buildResponse(
                exception.getMessage(),
                HttpStatus.BAD_REQUEST,
                request,
                model,
                redirectAttributes
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        log.warn(
                "Invalid request | method={} path={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                exception.getMessage()
        );

        return buildResponse(
                exception.getMessage(),
                HttpStatus.BAD_REQUEST,
                request,
                model,
                redirectAttributes
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        String message=exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error->error.getDefaultMessage())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(". "));

        if(message.isBlank()) {
            message="Please check the entered information.";
        }

        log.warn(
                "Validation exception | method={} path={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                message
        );

        return buildResponse(
                message,
                HttpStatus.BAD_REQUEST,
                request,
                model,
                redirectAttributes
        );
    }

    @ExceptionHandler(Exception.class)
    public Object handleGeneralException(
            Exception exception,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        log.error(
                "Unexpected error | method={} path={}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                "Something went wrong. Please try again.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request,
                model,
                redirectAttributes
        );
    }

    private Object buildResponse(
            String message,
            HttpStatus status,
            HttpServletRequest request,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        if(isApiRequest(request)) {

            return ResponseEntity
                    .status(status)
                    .body(
                            new ErrorResponse(
                                    LocalDateTime.now(),
                                    status.value(),
                                    status.getReasonPhrase(),
                                    message,
                                    request.getRequestURI()
                            )
                    );
        }

        redirectAttributes.addFlashAttribute("error",message);

        String backUrl=getBackUrl(request);

        if(!backUrl.equals("/auth/login")) {
            return "redirect:"+backUrl;
        }

        model.addAttribute("status",status.value());
        model.addAttribute("error",status.getReasonPhrase());
        model.addAttribute("message",message);
        model.addAttribute("path",request.getRequestURI());

        return "error";
    }

    private boolean isApiRequest(HttpServletRequest request) {

        String requestedWith=request.getHeader("X-Requested-With");
        String accept=request.getHeader("Accept");

        return request.getRequestURI().startsWith("/api/")
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept!=null && accept.contains("application/json"));
    }

    private String getBackUrl(HttpServletRequest request) {

        String referer=request.getHeader("Referer");

        if(referer!=null && !referer.isBlank()) {

            String contextPath=request.getContextPath();
            int index=referer.indexOf(contextPath);

            if(index>=0) {
                String path=referer.substring(
                        index+contextPath.length()
                );

                if(!path.isBlank()) {
                    return path;
                }
            }
        }

        return "/auth/login";
    }
}