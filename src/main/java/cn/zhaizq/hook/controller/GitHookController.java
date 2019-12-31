package cn.zhaizq.hook.controller;

import cn.zhaizq.hook.common.CommandExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api")
public class GitHookController {
    @PostMapping("/git/hook")
    public String hook(@RequestBody(required = false) String body) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.reader().readTree(body);
        String fullName = jsonNode.get("repository").get("full_name").asText();
        log.info("github webhooks -> {}", fullName);
        CommandExecutor.execute(
                "rm -rf /root/docker_mount/nginx/html/* /root/docker_mount/nginx/html/.git",
                "git clone https://github.com/ggboy-shakalaka/github-hook.git /root/docker_mount/nginx/html/");
        return "OK";
    }

    @GetMapping("/run/command")
    public String runCommand(@RequestParam("command") String command) throws IOException {
        CommandExecutor.execute(command);
        return "OK";
    }
}