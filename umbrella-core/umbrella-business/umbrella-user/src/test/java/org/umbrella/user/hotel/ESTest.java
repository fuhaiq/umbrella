package org.umbrella.user.hotel;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexRequest;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.umbrella.api.entity.TbHotel;
import org.umbrella.api.entity.es.TbHotelDoc;
import org.umbrella.user.service.ITbHotelService;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.List;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
public class ESTest {

    @Autowired
    private ITbHotelService tbHotelService;

    private ElasticsearchClient client;

    private RestClient restClient;


    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException, IOException {
        Path caCertificatePath = Paths.get("D:/docker-instances/elasticsearch/config/certs/http_ca.crt");
        CertificateFactory factory =
                CertificateFactory.getInstance("X.509");
        Certificate trustedCa;
        try (InputStream is = Files.newInputStream(caCertificatePath)) {
            trustedCa = factory.generateCertificate(is);
        }
        KeyStore trustStore = KeyStore.getInstance("pkcs12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca", trustedCa);
        SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                .loadTrustMaterial(trustStore, null);
        final SSLContext sslContext = sslContextBuilder.build();

        RestClientBuilder builder = RestClient.builder(
                        new HttpHost("localhost", 9200, "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setSSLContext(sslContext));

        Header[] defaultHeaders =
                new Header[]{new BasicHeader("Authorization",
                        "ApiKey cV8wbHlaQUJ5MS05RktHTThudEU6Smpydi1VWlhUNS14cEZnaDUxWm5vZw==")};
        builder.setDefaultHeaders(defaultHeaders);

        restClient = builder.build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());
        client = new ElasticsearchClient(transport);
    }

    @AfterEach
    void destroy() throws IOException {
        restClient.close();
    }

//    @Test
    @Order(0)
    void createIndex() throws IOException {
        var request = new CreateIndexRequest.Builder()
                .index("hotel")
                .withJson(Files.newInputStream(Paths.get("D:/GitHub/umbrella-core/doc/mapping.json")))
                .build();
        client.indices().create(request);
    }

//    @Test
    @Order(1)
    void existIndex() throws IOException {
        var request = new ExistsRequest.Builder()
                .index("hotel")
                .build();
        BooleanResponse exists = client.indices().exists(request);
        log.info("{}", exists);
    }


//    @Test
    @Order(2)
    void getIndex() throws IOException {
        var request = new GetIndexRequest.Builder()
                .index("hotel")
                .build();
        GetIndexResponse getIndexResponse = client.indices().get(request);
        log.info("{}", getIndexResponse);
    }

//    @Test
    @Order(3)
    void createDoc() throws IOException {

        TbHotel hotel = tbHotelService.getById(36934);
        TbHotelDoc doc = new TbHotelDoc(hotel);

        var request = new IndexRequest.Builder<>()
                .index("hotel")
                .id(doc.getId())
                .document(doc)
                .build();
        client.index(request);
    }

//    @Test
    @Order(3)
    void getDoc() throws IOException {

        GetRequest request = new GetRequest.Builder()
                .index("hotel")
                .id("36934")
                .build();

        GetResponse<TbHotelDoc> tbHotelDocGetResponse = client.get(request, TbHotelDoc.class);
        log.info("{}", tbHotelDocGetResponse.source());
    }

//    @Test
    @Order(4)
    void updateDoc() throws IOException {

        var doc = new TbHotelDoc();
        doc.setPrice(336);

        var request = new UpdateRequest.Builder<>()
                .index("hotel")
                .id("36934")
                .doc(doc)
                .build();
        client.update(request, TbHotelDoc.class);
    }


    @Test
    @Order(4)
    void bulkDoc() throws IOException {

        List<BulkOperation> list = tbHotelService.list().stream().map(TbHotelDoc::new)
                .map(doc -> new BulkOperation.Builder()
                        .create(d -> d.document(doc).index("hotel").id(doc.getId()))
                        .build()).toList();

        BulkRequest bulkRequest = new BulkRequest.Builder()
                .index("hotel")
                .operations(list)
                .build();

        client.bulk(bulkRequest);

    }
}
