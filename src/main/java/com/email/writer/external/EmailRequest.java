package com.email.writer.external;


import lombok.Data;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Data
public class EmailRequest {
    private String emailContent;
    private String tone;
}
