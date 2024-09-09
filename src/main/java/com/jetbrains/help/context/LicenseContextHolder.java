package com.jetbrains.help.context;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.XmlUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SignUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.json.JSONUtil;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.Set;

import static cn.hutool.crypto.asymmetric.SignAlgorithm.*;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LicenseContextHolder {

    public static String generateLicense(String licensesName, String assigneeName, String expiryDate, Set<String> productCodeSet) {
        String licenseId = IdUtil.fastSimpleUUID();
        List<Product> products = productCodeSet.stream()
                .map(productCode -> new Product()
                        .setCode(productCode)
                        .setFallbackDate(expiryDate)
                        .setPaidUpTo(expiryDate))
                .toList();
        LicensePart licensePart = new LicensePart()
                .setLicenseId(licenseId)
                .setLicenseeName(licensesName)
                .setAssigneeName(assigneeName)
                .setProducts(products);
        String licensePartJson = JSONUtil.toJsonStr(licensePart);
        String licensePartBase64 = Base64.encode(licensePartJson);
        PrivateKey privateKey = PemUtil.readPemPrivateKey(IoUtil.toStream(CertificateContextHolder.getPrivateKeyFile()));
        PublicKey publicKey = PemUtil.readPemPublicKey(IoUtil.toStream(CertificateContextHolder.getPublicKeyFile()));
        Certificate certificate = SecureUtil.readX509Certificate(IoUtil.toStream(CertificateContextHolder.getCrtFile()));
        Sign sign = SignUtil.sign(SHA1withRSA, privateKey.getEncoded(), publicKey.getEncoded());
        String signatureBase64 = Base64.encode(sign.sign(licensePartJson));
        String certBase64;
        try {
            certBase64 = Base64.encode(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Certificate extraction failed", e);
        }
        return CharSequenceUtil.format("{}-{}-{}-{}", licenseId, licensePartBase64, signatureBase64, certBase64);
    }

    @Data
    public static class LicensePart {

        private String licenseId;
        private String licenseeName;
        private String assigneeName;
        private List<Product> products;
        private String metadata = "0120230914PSAX000005";
    }

    @Data
    public static class Product {
        private String code;
        private String fallbackDate;
        private String paidUpTo;
    }

    public static String obtainTicket(ObtainTicketRequest request) {
        PrivateKey privateKey = PemUtil.readPemPrivateKey(IoUtil.toStream(CertificateContextHolder.getPrivateKeyFile()));
        PublicKey publicKey = PemUtil.readPemPublicKey(IoUtil.toStream(CertificateContextHolder.getPublicKeyFile()));
        Certificate cert = SecureUtil.readX509Certificate(IoUtil.toStream(CertificateContextHolder.getCrtFile()));
        Certificate licenseCert = SecureUtil.readX509Certificate(IoUtil.toStream(CertificateContextHolder.getLicenseCrtFile()));
        ObtainTicketResponse response = obtainTicket(privateKey, publicKey, cert, licenseCert, request);
        Document document = XmlUtil.beanToXml(response);
        String result = XmlUtil.toStr(document, "UTF-8", true, true);
        //!important 必须去除XML中所有的换行和空格，即紧凑模式
        result = result.replaceAll("[\n ]", "");
        Sign sign = SignUtil.sign(SHA1withRSA, privateKey.getEncoded(), publicKey.getEncoded());
        String sign64 = Base64.encode(sign.sign(result));
        String licenseCertBase64;
        try {
            licenseCertBase64 = Base64.encode(licenseCert.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Certificate extraction failed", e);
        }
        //<!--  SHA1withRSA-{下面 xml 主体内容的签名 }-{license Server CA}  -->
        String signRes = String.format("<!-- SHA1withRSA-%s-%s -->", sign64, licenseCertBase64);
        return signRes + "\n" + result;
    }

    public static ObtainTicketResponse obtainTicket(PrivateKey privateKey, PublicKey publicKey, Certificate cert, Certificate licenseCert, ObtainTicketRequest request) {
        String certBase64;
        String licenseCertBase64;
        try {
            certBase64 = Base64.encode(cert.getEncoded());
            licenseCertBase64 = Base64.encode(licenseCert.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Certificate extraction failed", e);
        }

        long time = System.currentTimeMillis();
        long leaseTime = time + (1000L * 60 * 60 * 24 * 365);
        //{失效时间戳}:{serverUid}
        String serverLease = String.format("%d:%s", leaseTime, CertificateContextHolder.SERVER_UID);

        Sign sign = SignUtil.sign(SHA1withRSA, privateKey.getEncoded(), publicKey.getEncoded());
        String signatureBase64 = Base64.encode(sign.sign(String.format("%d:%s", time, request.getMachineId())));
        //{时间戳}:{客户端提交的machineId}:SHA1withRSA:{对（{时间戳}:{客户端提交的machineId}）的签名}:{license Server CA}
        String confirmationStamp = String.format("%d:%s:SHA1withRSA:%s:%s",
                time, request.getMachineId(), signatureBase64, licenseCertBase64);

        sign = SignUtil.sign(SHA512withRSA, privateKey.getEncoded(), publicKey.getEncoded());
        byte[] bytes = sign.sign(serverLease);
        signatureBase64 = Base64.encode(bytes);
        //SHA512withRSA-{对「serverLease」的值进行签名}-{JetProfile CA}
        String leaseSignature = String.format("SHA512withRSA-%s-%s", signatureBase64, certBase64);
//        String leaseSignature = new BigInteger(1,bytes).toString();
        return ObtainTicketResponse.builder()
                .action(Action.NONE)
                .responseCode(ResponseCode.OK)
                .message(ResponseCode.OK.name())
                .ticketId("20")
                .ticketProperties(String.format("licensee=%s\tlicenseType=4\tmetadata=0120230914PSAX000005", request.getUserName()))
                .validationPeriod(71829000L)
                .validationDeadlinePeriod(-1)
                .prolongationPeriod(63462000L)//有效期
                .salt(request.getSalt())
                .serverUid(CertificateContextHolder.SERVER_UID)
                .serverLease(serverLease)
                .confirmationStamp(confirmationStamp)
                .leaseSignature(leaseSignature)
                .build();
    }

    public enum Action {
        NONE,
    }

    public enum ResponseCode {
        OK
    }

    /**
     * 获取认证票据
     *
     * @author Cikaros
     * @date 2023/11/1
     */
    @Data
    @Builder
    public static class ObtainTicketResponse {
        @Builder.Default
        private Action action = Action.NONE;
        private String confirmationStamp;
        private String leaseSignature;
        @Builder.Default
        private String message = "";
        @Builder.Default
        private Long prolongationPeriod = 1800000L;
        @Builder.Default
        private ResponseCode responseCode = ResponseCode.OK;
        private String salt;
        private String serverLease;
        private String serverUid;
        @Builder.Default
        private String ticketId = "1";
        private String ticketProperties;
        @Builder.Default
        private Integer validationDeadlinePeriod = -1;
        @Builder.Default
        private Long validationPeriod = 1800000L;

    }

    /**
     * @author Cikaros
     * @date 2023/10/20
     */
    @Data
    public static class ObtainTicketRequest {

        private String ideProductCode;

        private Boolean empty;

        private String buildDate;

        private String buildNumber;

        private Integer clientVersion;

        private String hostName;

        private String machineId;

        private String productCode;

        private String productFamilyId;

        private String salt;

        private Boolean secure;

        private String userName;

        private String version;

        private Long versionNumber;

    }
}
