package org.common.db.config;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class EncryptDecrypt {

    public static String encryptPassword(String pwd) {
        String publicKy = "keyhere";
        String encPwd = "";
        try {
            RSAPublicKey rsaPublicKey = EncryptDecrypt.base64StringToPublicKey(publicKy);
            encPwd = EncryptDecrypt.encryptData(pwd, rsaPublicKey);
            System.out.println("encryptedData " + encPwd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encPwd;
    }

    public static String decryptPassword(String encPwd) {
        String privateKy = "keyhere";
        String pwd = "";
        try {
            RSAPrivateKey rsaPrivateKey = EncryptDecrypt.base64StringToPrivateKey(privateKy);
            pwd = EncryptDecrypt.decryptData(encPwd, rsaPrivateKey);
            // System.out.println("decryptedData " + pwd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pwd;
        // decrypt
    }

    public static RSAPrivateKey base64StringToPrivateKey(String base64PrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(spec);
    }

    public static RSAPublicKey base64StringToPublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(x509EncodedKeySpec);
    }

    public static String encryptData(String plainData, PublicKey publicKey) throws Exception {
        Cipher inputCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        inputCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.getEncoder().encodeToString(inputCipher.doFinal(plainData.getBytes()));
    }

    public static String decryptData(String encryptedDataBase64, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] bytes = cipher.doFinal(Base64.getDecoder().decode(encryptedDataBase64));
        return new String(bytes);
    }
}
