package de.craftsblock.craftsnet.addon.loaders;

import de.craftsblock.craftscore.utils.id.Snowflake;
import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.meta.RegisteredService;
import de.craftsblock.craftsnet.logging.Logger;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

/**
 * The {@code ArtifactLoader} class provides functionality for resolving and loading libraries (artifacts)
 * for addons in a modular system.
 *
 * @author CraftsBlock
 * @author Philipp Maywald
 * @version 2.0.3
 * @see <a href="https://maven.apache.org/resolver/index.html">Eclipse Aether</a>
 * @since 3.0.0-SNAPSHOT
 */
public final class ArtifactLoader {

    private final RepositorySystem repository;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;
    private final RemoteRepository defaultRepo;

    /**
     * Creates a new instance of the artifact loader
     */
    public ArtifactLoader() {
        RepositorySystemSupplier repositorySupplier = new RepositorySystemSupplier();
        repository = repositorySupplier.get();

        // TODO: Find a better solution / work around to create a RepositorySystemSession
        //       Update during / before 4.0.0 release of CraftsNet
//        MavenSessionBuilderSupplier sessionSupplier = new MavenSessionBuilderSupplier(repository);
//        RepositorySystemSession.CloseableSession closeableSession = sessionSupplier.get().build();
//        session = new DefaultRepositorySystemSession(closeableSession);

        session = MavenRepositorySystemUtils.newSession();
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        session.setLocalRepositoryManager(repository.newLocalRepositoryManager(session, new LocalRepository("libraries")));
        session.setReadOnly();

        repositories = repository.newResolutionRepositories(
                session,
                Collections.singletonList(
                        defaultRepo = new RemoteRepository.Builder(
                                "central",
                                "default",
                                "https://repo.maven.apache.org/maven2"
                        ).build()
                )
        );
    }

    /**
     * Cleanup internal repository cache
     */
    public void cleanup() {
        repositories.parallelStream()
                .filter(remoteRepository -> !remoteRepository.equals(defaultRepo))
                .forEach(repositories::remove);
    }

    /**
     * Cleanup and shutdown of the internal repository resolver
     */
    public void stop() {
        cleanup();
        repository.shutdown();
    }

    /**
     * Adds a remote repository to the list of repositories used for dependency resolution.
     *
     * @param repo The URL of the remote repository to be added.
     */
    public void addRepository(String repo) {
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
     * @param addonLoader The addon loader responsible for loading services.
     * @param services    The list with the currently registered services.
     * @param addon       The name of the addon for which libraries are being loaded.
     * @param libraries   The coordinates of the libraries to be loaded.
     * @return An array of URLs representing the loaded libraries.
     */
    public URL[] loadLibraries(CraftsNet craftsNet, AddonLoader addonLoader, Collection<RegisteredService> services, String addon, String... libraries) {
        Logger logger = craftsNet.logger();
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

        List<URL> urls = new ArrayList<>();
        result.getArtifactResults().forEach(artifact -> {
            File file = artifact.getArtifact().getPath().toFile();
            try (JarFile jarFile = new JarFile(file, true, ZipFile.OPEN_READ, Runtime.version())) {
                urls.add(file.toURI().toURL());
                services.addAll(addonLoader.loadServices(jarFile));
            } catch (IOException e) {
                logger.error(e, "Error while loading libraries for " + addon);
                return;
            }
            logger.debug("Loaded library " + file.getName() + " for " + addon);
        });

        return urls.toArray(URL[]::new);
    }

}