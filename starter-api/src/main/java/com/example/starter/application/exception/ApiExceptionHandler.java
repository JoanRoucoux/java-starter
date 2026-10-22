package com.example.starter.application.exception;

import com.example.starter.domain.exception.business.BusinessException;
import com.example.starter.domain.exception.technical.TechnicalException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setTitle("Business rule violated");
        return problem;
    }

    @ExceptionHandler(TechnicalException.class)
    ProblemDetail handleTechnical(TechnicalException exception) {
        log.error("upstream dependency failed: {}", exception.getMessage(), exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, exception.getMessage());
        problem.setTitle("Upstream dependency failed");
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    void handleInvalidParameter(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }
}
