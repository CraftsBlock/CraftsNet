package de.craftsblock.craftsnet.api.ssl;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.utils.PassphraseUtils;
import de.craftsblock.craftsnet.utils.SecureEncodingUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * The SSL class encapsulates a collection of utility methods aimed at facilitating SSL/TLS functionality
 * within an application. Serving as a central hub for SSL configuration management, this class offers
 * a range of methods for loading SSL contexts. These methods enable developers to seamlessly handle
 * SSL-related operations, including the loading of SSL contexts using either default or custom
 * certificate chain and private key files.
 *
 * @author Philipp Maywald
 * @author CraftsBlock
 * @since 2.1.1-SNAPSHOT
 */
public class SSL {

    /**
     * This method loads SSL context using default paths for the certificate chain and private key files.
     * The default paths are "./certificates/fullchain.pem" for the certificate chain and
     * "./certificates/privkey.pem" for the private key.
     *
     * @param craftsNet The CraftsNet instance which instantiates this ssl load operation.
     * @return The SSLContext instance loaded with the default certificate chain and private key.
     * @throws CertificateException      If there's an error with the certificate.
     * @throws IOException               If there's an I/O error.
     * @throws NoSuchAlgorithmException  If the algorithm used is not available.
     * @throws KeyStoreException         If there's an error with the keystore.
     * @throws KeyManagementException    If there's an error with the key management.
     * @throws UnrecoverableKeyException If the key is unrecoverable.
     */
    public static SSLContext load(CraftsNet craftsNet)
            throws CertificateException, IOException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException,
            UnrecoverableKeyException, InvalidKeySpecException {
        return load(craftsNet, "./certificates/fullchain.pem", "./certificates/privkey.pem");
    }

    /**
     * This method loads an SSL context using the provided full chain and private key files.
     * It sets up an SSLContext with the certificate chain and private key from the given files.
     *
     * @param craftsNet The CraftsNet instance which instantiates this ssl load operation.
     * @param fullchain The path to the full chain file containing the certificate chain.
     * @param privkey   The path to the private key file.
     * @return The SSLContext instance loaded with the provided certificate chain and private key.
     * @throws CertificateException      If there's an error with the certificate.
     * @throws IOException               If there's an I/O error.
     * @throws NoSuchAlgorithmException  If the algorithm used is not available.
     * @throws KeyStoreException         If there's an error with the keystore.
     * @throws KeyManagementException    If there's an error with the key management.
     * @throws UnrecoverableKeyException If the key is unrecoverable.
     */
    public static SSLContext load(CraftsNet craftsNet, String fullchain, String privkey)
            throws CertificateException, IOException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException,
            UnrecoverableKeyException, InvalidKeySpecException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);

        File certFile = file(fullchain);
        File keyFile = file(privkey);

        byte[] passphrase = PassphraseUtils.generateSecure(55, 75, true);
        char[] password = SecureEncodingUtils.decode(passphrase, StandardCharsets.UTF_8);
        PassphraseUtils.erase(passphrase);

        try (InputStream certStream = new FileInputStream(certFile);
             InputStream keyStream = new FileInputStream(keyFile)) {

            X509Certificate[] chain = getCertificateChain(certStream);

            if (chain == null || chain.length == 0) {
                craftsNet.getLogger().error(
                        "Your fullchain (%s) does not contain any certificates!",
                        fullchain);
                return null;
            }

            PrivateKey privateKey = getPrivateKey(keyStream);

            for (X509Certificate cert : chain) {
                cert.checkValidity();

                try {
                    cert.checkValidity(Date.from(OffsetDateTime.now().plusDays(30).toInstant()));
                } catch (Exception e) {
                    craftsNet.getLogger().warning(
                            "The lifespan of your certificate is less than 30 days!");
                }
            }

            keyStore.setKeyEntry("privateKey", privateKey, password, chain);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);

        PassphraseUtils.erase(password);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                new SecureRandom()
        );

        return sslContext;
    }

    /**
     * Returns a File object for the given path and ensures that it exists.
     *
     * @param path Path to the file.
     * @return File instance representing the path.
     * @throws FileNotFoundException If the file does not exist.
     */
    private static File file(String path) throws FileNotFoundException {
        File file = new File(path);

        if (!file.exists()) {
            throw new FileNotFoundException("File " + file.getAbsolutePath() + " not found!");
        }

        return file;
    }

    /**
     * Parses a PEM encoded certificate chain into X509Certificate objects.
     *
     * @param chainStream InputStream containing PEM encoded certificate chain.
     * @return Array of X509Certificates or {@code null} if none found.
     * @throws IOException          If reading the stream fails.
     * @throws CertificateException If certificate parsing fails.
     */
    private static X509Certificate[] getCertificateChain(InputStream chainStream)
            throws IOException, CertificateException {

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certs = new ArrayList<>();

        StringBuilder pem = new StringBuilder();
        boolean inside = false;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(chainStream))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (line.contains("BEGIN CERTIFICATE")) {
                    inside = true;
                    continue;
                }

                if (line.contains("END CERTIFICATE")) {
                    byte[] decoded = Base64.getDecoder().decode(pem.toString());
                    Certificate cert = factory.generateCertificate(new ByteArrayInputStream(decoded));

                    certs.add((X509Certificate) cert);
                    pem.setLength(0);
                    inside = false;
                    continue;
                }

                if (inside) {
                    pem.append(line.trim());
                }
            }
        }

        return certs.isEmpty() ? null : certs.toArray(X509Certificate[]::new);
    }

    /**
     * Parses a PEM encoded PKCS#8 private key.
     *
     * @param privateKeyStream InputStream containing the PEM encoded private key.
     * @return The decoded PrivateKey or {@code null} if parsing fails.
     */
    private static PrivateKey getPrivateKey(InputStream privateKeyStream)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(privateKeyStream))) {
            StringBuilder pem = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("-")) {
                    continue;
                }

                pem.append(line.trim());
            }

            byte[] decoded = Base64.getDecoder().decode(pem.toString());
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        }
    }

}
