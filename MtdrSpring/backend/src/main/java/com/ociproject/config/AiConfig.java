package com.ociproject.config;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.generativeaiinference.GenerativeAiInferenceClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    public RestTemplate aiRestTemplate() {
        RequestConfig reqCfg = RequestConfig.custom()
                .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(30))
                .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(90))
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(reqCfg)
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }

    @Bean
    @Lazy
    public GenerativeAiInferenceClient ociGenerativeAiInferenceClient(AiProperties props) throws IOException {
        String path = expandHome(props.getOci().getConfigFilePath());
        ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(path, props.getOci().getAuthProfile());
        AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);
        GenerativeAiInferenceClient client = GenerativeAiInferenceClient.builder()
                .build(provider);
        if (props.getOci().getRegion() != null && !props.getOci().getRegion().isBlank()) {
            client.setRegion(Region.fromRegionCodeOrId(props.getOci().getRegion()));
        }
        return client;
    }

    private static String expandHome(String path) {
        if (path == null) return null;
        if (path.startsWith("~/") || path.startsWith("~\\")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }
}
