package de.probstl.ausgaben.aws;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import de.probstl.ausgaben.data.Expense;
import reactor.core.publisher.Mono;

@Service
@PropertySource("classpath:awsConnection.properties")
public class AwsService {

    /** Logger */
    private static Logger logger = LoggerFactory.getLogger(AwsService.class);

    /** The hostname the http request is sent to */
    @Value("${aws.hostname}")
    private String hostname;

    /** The http endpoint */
    @Value("${aws.endpoint}")
    private String endpoint;

    /** The AWS region */
    @Value("${aws.region}")
    private String region;

    /** The AWS service type */
    @Value("${aws.service}")
    private String service;

    /** The AWS secret access key */
    @Value("${awsSecretAccessKey}")
    private String secretAccessKey;

    /** The AWS access key id */
    @Value("${awsAccessKeyId}")
    private String accessKeyId;

    /** The URL part appended the endpoint and part of canonical request */
    @Value("${aws.canonicalUri}")
    private String canonicalUri;

    /**
     * Send the expense to an AWS API Gateway configured
     * 
     * @param expense The expense to be sent
     * @return Was the post successful
     */
    public boolean sendExpense(Expense expense) {

        WebClient webClient = WebClient.builder().filter(logRequest()).filter(logResponse()).baseUrl(this.endpoint)
                .build();

        ZonedDateTime requestDate = ZonedDateTime.now(ZoneId.of("UTC"));
        String dateStr = DateTimeFormatter.ofPattern("yyyyMMdd").format(requestDate);
        String dateTimeStr = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").format(requestDate);
        String credentialScope = dateStr.concat("/").concat(this.region).concat("/").concat(this.service)
                .concat("/aws4_request");

        String payload = "{" + "\"KeySchema\": [" + "{" + "\"KeyType\": \"HASH\"," + "\"AttributeName\": \"Id\"" + "}"
                + "]," + "\"TableName\": \"TestTable\"," + "\"AttributeDefinitions\": [" + "{"
                + "\"AttributeName\": \"Id\"," + "\"AttributeType\": \"S\"" + "}" + "],"
                + "\"ProvisionedThroughput\": {" + "\"WriteCapacityUnits\": 5," + "\"ReadCapacityUnits\": 5" + "}"
                + "}";
        String payloadHashStr = DigestUtils.sha256Hex(payload);

        // ************* TASK 1: CREATE A CANONICAL REQUEST *************
        // http://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
        StringBuilder canonicalRequestStr = new StringBuilder();
        canonicalRequestStr.append("POST\n"); // HTTPRequestMethod
        canonicalRequestStr.append(this.canonicalUri).append("\n"); // CanonicalURI
        canonicalRequestStr.append("\n"); // CanonicalQueryString
        canonicalRequestStr.append("content-type:application/json\n"); // CanonicalHeaders
        canonicalRequestStr.append("host:").append(this.hostname).append("\n");
        canonicalRequestStr.append("x-amz-date:").append(dateTimeStr).append("\n\n");
        canonicalRequestStr.append("content-type;host;x-amz-date\n"); // SignedHeaders
        canonicalRequestStr.append(payloadHashStr); // HexEncode(Hash(<emptyString>))
        String hashedCanonicalRequest = DigestUtils.sha256Hex(canonicalRequestStr.toString());

        // ************* TASK 2: CREATE THE STRING TO SIGN*************
        // Match the algorithm to the hashing algorithm SHA-256
        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append("AWS4-HMAC-SHA256\n");
        stringToSign.append(dateTimeStr).append("\n");
        stringToSign.append(credentialScope).append("\n");
        stringToSign.append(hashedCanonicalRequest);

        // ************* TASK 3: CALCULATE THE SIGNATURE *************
        // Create the signing key
        byte[] hmacDate = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, "AWS4" + secretAccessKey).hmac(dateStr);
        byte[] hmacRegion = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, hmacDate).hmac(this.region);
        byte[] hmacService = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, hmacRegion).hmac(this.service);
        byte[] signingKey = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, hmacService).hmac("aws4_request");
        String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, signingKey).hmacHex(stringToSign.toString());

        // ************* TASK 4: ADD SIGNING INFORMATION TO THE REQUEST *************
        StringBuilder authorizationStr = new StringBuilder();
        authorizationStr.append("AWS4-HMAC-SHA256 ");
        authorizationStr.append("Credential=");
        authorizationStr.append(accessKeyId).append("/").append(credentialScope).append(", ");
        authorizationStr.append("SignedHeaders=content-type;host;x-amz-date, ");
        authorizationStr.append("Signature=").append(signature);

        String result = "";
        try {
            result = webClient.post().uri(this.canonicalUri).accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(payload))
                    .header("Authorization", authorizationStr.toString()).header("x-amz-date", dateTimeStr)
                    .header("host", this.hostname).retrieve().bodyToMono(String.class).block();
        } catch (Exception e) {
            logger.warn("call failed", e);
        }

        logger.info("Response: {}", result);
        return true;
    }

    public ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            logMethodAndUrl(request);
            logHeaders(request);

            return Mono.just(request);
        });
    }

    public ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            logStatus(response);
            logHeaders(response);

            return logBody(response);
        });
    }

    private void logStatus(ClientResponse response) {
        HttpStatus status = response.statusCode();
        logger.info("Returned staus code {} ({})", status.value(), status.getReasonPhrase());
    }

    private Mono<ClientResponse> logBody(ClientResponse response) {
        if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
            return response.bodyToMono(String.class).flatMap(body -> {
                logger.info("Response body: {}", body);
                return Mono.error(new RuntimeException("Status " + response.rawStatusCode()));
            });
        } else {
            return Mono.just(response);
        }
    }

    private void logHeaders(ClientResponse response) {
        response.headers().asHttpHeaders().forEach((name, values) -> {
            values.forEach(value -> {
                logNameAndValuePair(name, value);
            });
        });
    }

    private void logHeaders(ClientRequest request) {
        request.headers().forEach((name, values) -> {
            values.forEach(value -> {
                logNameAndValuePair(name, value);
            });
        });
    }

    private void logNameAndValuePair(String name, String value) {
        if (logger.isDebugEnabled()) {
            logger.debug("{}={}", name, value);
        }
    }

    private void logMethodAndUrl(ClientRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.method().name());
        sb.append(" to ");
        sb.append(request.url());
        logger.info("WebClient call: {}", sb);
    }
}
