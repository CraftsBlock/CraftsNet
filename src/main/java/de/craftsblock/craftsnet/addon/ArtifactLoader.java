package de.craftsblock.craftsnet.addon;

import de.craftsblock.craftscore.id.Snowflake;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.utils.Logger;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The {@code ArtifactLoader} class provides functionality for resolving and loading libraries (artifacts)
 * for addons in a modular system.
 *
 * @author CraftsBlock
 * @version 1.0.0
 * @apiNote It utilizes the Eclipse Aether library for handling dependency resolution and management.
 * @see <a href="https://maven.apache.org/resolver/index.html">Eclipse Aether</a>
 * @since 3.0.0
 */
class ArtifactLoader {

    private static final Logger logger = CraftsNet.logger();

    private static final RepositorySystem repository;
    private static final DefaultRepositorySystemSession session;
    private static final List<RemoteRepository> repositories;

    static {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        repository = locator.getService(RepositorySystem.class);
        session = MavenRepositorySystemUtils.newSession();

        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        session.setLocalRepositoryManager(repository.newLocalRepositoryManager(session, new LocalRepository("libraries")));
        session.setReadOnly();

        repositories = repository.newResolutionRepositories(
                session,
                Collections.singletonList(
                        new RemoteRepository.Builder(
                                "central",
                                "default",
                                "https://repo.maven.apache.org/maven2"
                        ).build()
                )
        );
    }

    /**
     * Adds a remote repository to the list of repositories used for dependency resolution.
     *
     * @param repo The URL of the remote repository to be added.
     */
    protected static void addRepository(String repo) {
        if (repositories.stream().anyMatch(repository -> Objects.equals(repository.getUrl(), repo))) return;
        RemoteRepository remoteRepository = new RemoteRepository.Builder(
                Snowflake.generate() + "",
                "default",
                repo
        ).build();
        repositories.addAll(repository.newResolutionRepositories(session, Collections.singletonList(remoteRepository)));
    }

    /**
     * Loads libraries for a specific addon based on the provided library coordinates.
     *
     * @param addonLoader   The addon loader responsible for loading services.
     * @param configuration The configuration for the addon loader.
     * @param addon         The name of the addon for which libraries are being loaded.
     * @param libraries     The coordinates of the libraries to be loaded.
     * @return An array of URLs representing the loaded libraries.
     */
    protected static URL[] loadLibraries(AddonLoader addonLoader, AddonLoader.Configuration configuration, String addon, String... libraries) {
        logger.debug("Loading " + libraries.length + " libraries for " + addon + "...");
        List<Dependency> dependencies = new ArrayList<>();
        for (String library : libraries) {
            DefaultArtifact artifact = new DefaultArtifact(library);
            Dependency dependency = new Dependency(artifact, null);
            dependencies.add(dependency);
        }

        DependencyResult result;
        try {
            result = repository.resolveDependencies(session, new DependencyRequest(new CollectRequest((Dependency) null, dependencies, repositories), null));
        } catch (DependencyResolutionException e) {
            logger.error(e, "Error while loading libraries for " + addon);
            return new URL[0];
        }

        ConcurrentLinkedQueue<URL> urls = new ConcurrentLinkedQueue<>();
        result.getArtifactResults().forEach(artifact -> {
            File file = artifact.getArtifact().getFile();
            try {
                urls.add(file.toURI().toURL());
                configuration.services().addAll(addonLoader.loadServices(file));
            } catch (IOException e) {
                logger.error(e, "Error while loading libraries for " + addon);
                return;
            }
            logger.debug("Loaded library " + file.getName() + " for " + addon);
        });

        return urls.toArray(new URL[0]);
    }

}