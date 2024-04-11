package com.jetbrains.help.context;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SignUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.json.JSONUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.Set;

import static cn.hutool.crypto.asymmetric.SignAlgorithm.SHA1withRSA;

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
        PrivateKey privateKey = PemUtil.readPemPrivateKey(IoUtil.toStream(CertificateContextHolder.privateKeyFile()));
        PublicKey publicKey = PemUtil.readPemPublicKey(IoUtil.toStream(CertificateContextHolder.publicKeyFile()));
        Certificate certificate = SecureUtil.readX509Certificate(IoUtil.toStream(CertificateContextHolder.crtFile()));
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

}
