package de.craftsblock.craftsnet.utils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class SSL {

    public static SSLContext load(String fullchain, String privkey, String key) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        try (InputStream fullchainStream = new FileInputStream(file(fullchain));
             InputStream privateKeyStream = new FileInputStream(file(privkey))) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(fullchainStream);
            PrivateKey privateKey = getPrivateKey(privateKeyStream);
            keyStore.setCertificateEntry("certificate", certificate);
            keyStore.setKeyEntry("privateKey", privateKey, key.toCharArray(), new Certificate[]{certificate});
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, key.toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    private static File file(String path) throws FileNotFoundException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        if (!file.exists())
            throw new FileNotFoundException("File " + file.getAbsolutePath() + " not found!");
        return file;
    }

    private static PrivateKey getPrivateKey(InputStream privateKeyStream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(privateKeyStream))) {
            String line;
            StringBuilder key = new StringBuilder();
            while ((line = br.readLine()) != null) if (!line.startsWith("-")) key.append(line);
            byte[] decodedKey = Base64.getDecoder().decode(key.toString());
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
