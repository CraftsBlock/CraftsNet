package de.craftsblock.craftsnet.utils;

import com.google.gson.Gson;
import de.craftsblock.craftsnet.CraftsNet;

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

/**
 * The SSL class provides utility methods to set up an SSL context based on the provided SSL certificate and private key.
 * It allows for secure connections using the TLS protocol. The class supports loading X.509 certificates and RSA private keys
 * from files and initializes the SSL context using these credentials. Additionally, the class provides helper methods for
 * file handling and private key extraction.
 *
 * @author CraftsBlock
 * @version 1.0
 * @since 2.1.1
 */
public class SSL {

    /**
     * Loads an SSL context based on the provided SSL certificate and private key files.
     *
     * @param fullchain The path to the file containing the SSL certificate (X.509 format).
     * @param privkey   The path to the file containing the private key (RSA format).
     * @param key       The key which is used to secure the private key while running.
     * @return SSLContext An initialized SSLContext object for secure connections.
     * @throws CertificateException      If there is an error with the certificate.
     * @throws IOException               If there is an I/O related error.
     * @throws NoSuchAlgorithmException  If a required cryptographic algorithm is not available.
     * @throws KeyStoreException         If there is an error with the KeyStore.
     * @throws KeyManagementException    If there is an error with SSL context initialization.
     * @throws UnrecoverableKeyException If the private key cannot be recovered.
     */
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

    /**
     * Helper method to get a File object from the given path and create parent directories if necessary.
     *
     * @param path The path to the file.
     * @return File A File object representing the file specified by the path.
     * @throws FileNotFoundException If the specified file is not found.
     */
    private static File file(String path) throws FileNotFoundException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        if (!file.exists())
            throw new FileNotFoundException("File " + file.getAbsolutePath() + " not found!");
        return file;
    }

    /**
     * Helper method to read the private key from an InputStream and convert it to a PrivateKey object.
     *
     * @param privateKeyStream The InputStream containing the private key data.
     * @return PrivateKey A PrivateKey object representing the private key.
     * @throws IOException If there is an I/O related error.
     */
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
                CraftsNet.logger.error(e);
            }
        }
        return null;
    }

}
