package com.tripify.communication_service.client;

import com.tripify.communication_service.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-auth-service", url = "${user-auth.service.url:http://localhost:8081}", configuration = FeignClientConfig.class)
public interface UserProfileClient {

    @PostMapping("/api/v1/profile/users/names")
    Map<String, String> resolveNames(@RequestBody List<String> subs);
}
