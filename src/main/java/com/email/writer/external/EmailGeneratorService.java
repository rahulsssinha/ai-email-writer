package com.email.writer.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {


    private final WebClient webClient;
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApikey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }


    @PostMapping("/generate")
    public String generateEmailReply(EmailRequest emailRequest){
        //Build the promt
        String prompt=buildPrompt(emailRequest);

        //Craft a request
        Map<String,Object> requestBody= Map.of(
               "contents", new Object[] {
                       Map.of("parts", new Object[]{
                               Map.of("text", prompt)
                       })
                }
        );

        //Do request and get response
        String response = webClient.post()
                .uri(geminiApiUrl + "?key=" +geminiApikey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();


        //return response
        return extractedResponseContent(response);
    }

    private String extractedResponseContent(String response) {
        try{
            ObjectMapper mapper =new ObjectMapper();
            JsonNode rootNode =mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        }catch(Exception e){
            return "Error processing request"+ e.getMessage();
        }
    }



    private String buildPrompt(EmailRequest emailRequest) {
        if (emailRequest == null || emailRequest.getEmailContent() == null || emailRequest.getEmailContent().isBlank()) {
            return """
        Provide a professional email template with placeholders. 
        Output ONLY the email content—no explanations, no preamble.
        Divide the email into:
        - Greeting
        - Purpose/Main Content
        - Call to Action
        - Closing
        - Signature
        Do not include a subject line.
        """;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email based on the following content. ");
        prompt.append("Output ONLY the email, do NOT include any explanation or preamble or subject. ");
        if (emailRequest.getTone() != null && !emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone. ");
        }
        prompt.append("\nOriginal email content: ").append(emailRequest.getEmailContent());

        return prompt.toString();
    }



}
