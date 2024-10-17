package de.craftsblock.craftsnet.utils;

import de.craftsblock.craftsnet.CraftsNet;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
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
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 1.2.1
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
    public static SSLContext load(CraftsNet craftsNet) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
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
    public static SSLContext load(CraftsNet craftsNet, String fullchain, String privkey) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);

        File privkeyFile = file(privkey);
        String key = Utils.secureRandomPassphrase();
        try (InputStream fullchainStream = new FileInputStream(file(fullchain));
             InputStream privateKeyStream = new FileInputStream(privkeyFile)) {
            X509Certificate[] certificates = getCertificateChain(fullchainStream);
            if (certificates == null || certificates.length != 2) {
                craftsNet.logger().error("Your fullchain (" + fullchain + ") is not valid. Expected Certificates: 2, Got: " + (certificates != null ? certificates.length : "null"));
                return null;
            }

            PrivateKey privateKey = getPrivateKey(craftsNet, privateKeyStream);

            try {
                for (X509Certificate certificate : certificates) {
                    certificate.checkValidity();
                    try {
                        certificate.checkValidity(Date.from(OffsetDateTime.now().plusDays(30).toInstant()));
                    } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                        craftsNet.logger().warning("The lifespan of your certificate is less than 30 days!");
                    }
                }
            } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                craftsNet.logger().error(e, "Could not activate ssl!");
                return null;
            }

            try {
                if (!verify(certificates[0], privateKey))
                    throw new InvalidKeyException("The value signed with the private key could be verified with the public key!");
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | NullPointerException e) {
                craftsNet.logger().error(e, "Could not activate ssl: There was an unexpected exception while verifying the key pair!");
                return null;
            }

            keyStore.setKeyEntry("privateKey", privateKey, key.toCharArray(), certificates);
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
     * This method verifies the authenticity of a certificate using a given private key.
     * It generates a challenge, signs it with the private key, and then verifies the signature
     * using the public key extracted from the provided certificate.
     *
     * @param certificate The certificate to verify.
     * @param privateKey  The private key used to sign the challenge.
     * @return true if the signature is successfully verified, false otherwise.
     * @throws NoSuchAlgorithmException If the algorithm used for signature verification is not available.
     * @throws InvalidKeyException      If the private key or public key is invalid.
     * @throws SignatureException       If an error occurs during signature verification.
     */
    private static boolean verify(Certificate certificate, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] challenge = new byte[32];
        SecureRandom random = SecureRandom.getInstanceStrong();
        random.nextBytes(challenge);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(challenge);
        byte[] signature = sig.sign();

        sig.initVerify(certificate.getPublicKey());
        sig.update(challenge);

        return sig.verify(signature);
    }

    /**
     * This method creates a File instance for the given file path.
     * If the file does not exist, it throws a FileNotFoundException.
     * It also creates any necessary parent directories for the file.
     *
     * @param path The path to the file.
     * @return The File instance representing the file.
     * @throws FileNotFoundException If the file does not exist.
     */
    private static File file(String path) throws FileNotFoundException {
        File file = new File(path);
        file.getParentFile().mkdirs();
        if (!file.exists())
            throw new FileNotFoundException("File " + file.getAbsolutePath() + " not found!");
        return file;
    }

    /**
     * This method reads an InputStream containing a certificate chain in PEM format,
     * decodes it, and returns an array of X509Certificate instances representing the chain.
     *
     * @param chainStream An InputStream containing the certificate chain data in PEM format.
     * @return An array of X509Certificate instances representing the certificate chain.
     * @throws IOException          If an I/O error occurs while reading the input stream.
     * @throws CertificateException If an error occurs while generating the certificate chain.
     */
    private static X509Certificate[] getCertificateChain(InputStream chainStream) throws IOException, CertificateException {
        List<X509Certificate> certificates = new ArrayList<>();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(chainStream))) {
            if (!br.readLine().contains("BEGIN CERTIFICATE")) return null;
            String line;
            StringBuilder certContent = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.contains("END CERTIFICATE")) {
                    byte[] cert = Base64.getDecoder().decode(certContent.toString());
                    Certificate certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(cert));
                    certificates.add((X509Certificate) certificate);
                    certContent.delete(0, certContent.length());
                } else if (!line.contains("----")) certContent.append(line);
            }
        }
        return certificates.toArray(X509Certificate[]::new);
    }

    /**
     * This method reads an InputStream containing a private key in PEM format,
     * decodes it, and returns a PrivateKey instance.
     *
     * @param craftsNet        The CraftsNet instance which instantiates this ssl private key operation.
     * @param privateKeyStream An InputStream containing the private key data in PEM format.
     * @return The PrivateKey instance decoded from the provided input stream.
     * @throws IOException If an I/O error occurs while reading the input stream.
     */
    private static PrivateKey getPrivateKey(CraftsNet craftsNet, InputStream privateKeyStream) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(privateKeyStream))) {
            String line;
            StringBuilder key = new StringBuilder();
            while ((line = br.readLine()) != null) if (!line.startsWith("-")) key.append(line);
            byte[] decodedKey = Base64.getDecoder().decode(key.toString());
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(decodedKey));
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                craftsNet.logger().error(e);
            }
        }
        return null;
    }

}
