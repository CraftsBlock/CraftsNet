package de.craftsblock.craftsnet.addon.artifacts;

import de.craftsblock.craftsnet.CraftsNet;
import de.craftsblock.craftsnet.addon.loaders.AddonLoader;
import de.craftsblock.craftsnet.addon.meta.RegisteredService;
import de.craftsblock.craftsnet.logging.Logger;
import org.apache.maven.repository.internal.artifact.FatArtifactTraverser;
import org.apache.maven.repository.internal.type.DefaultTypeProvider;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
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
import org.eclipse.aether.util.artifact.DefaultArtifactTypeRegistry;
import org.eclipse.aether.util.filter.OrDependencyFilter;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.util.repository.SimpleArtifactDescriptorPolicy;

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
 * @version 2.1.2
 * @see <a href="https://maven.apache.org/resolver/index.html">Eclipse Aether</a>
 * @since 3.0.0-SNAPSHOT
 */
public final class ArtifactLoader {

    private final RepositorySystem repository;
    private final RepositorySystemSession.CloseableSession session;
    private final List<RemoteRepository> repositories;
    private final List<RemoteRepository> defaultRepos;

    /**
     * Creates a new instance of the artifact loader
     */
    @SuppressWarnings("deprecation")
    public ArtifactLoader() {
        RepositorySystemSupplier repositorySupplier = new RepositorySystemSupplier();
        repository = repositorySupplier.get();

        DefaultArtifactTypeRegistry stereotypes = new DefaultArtifactTypeRegistry();
        new DefaultTypeProvider().types().forEach(stereotypes::add);

        session = repository.createSessionBuilder()
                .setDependencyTraverser(new FatArtifactTraverser())
                .setDependencyManager(new ClassicDependencyManager())
                .setDependencySelector(new AndDependencySelector(
                        new ScopeDependencySelector("test", "provided"),
                        new OptionalDependencySelector(),
                        new ExclusionDependencySelector()))
                .setArtifactTypeRegistry(stereotypes)
                .setArtifactDescriptorPolicy(new SimpleArtifactDescriptorPolicy(true, true))

                .setIgnoreArtifactDescriptorRepositories(false)
                .withLocalRepositories(new LocalRepository("libraries"))
                .setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL)
                .setSystemProperties(System.getProperties())
                .build();

        defaultRepos = List.of(
                createRepository("central", "https://repo.maven.apache.org/maven2"),
                createRepository("https://repo.craftsblock.de/releases"),
                createRepository("https://repo.craftsblock.de/experimental")
        );

        repositories = repository.newResolutionRepositories(session, defaultRepos);
    }

    /**
     * Cleanup internal repository cache
     */
    public void cleanup() {
        repositories.removeIf(remoteRepository -> !defaultRepos.contains(remoteRepository));
    }

    /**
     * Cleanup and shutdown of the internal repository resolver
     */
    public void stop() {
        cleanup();
        session.close();
        repository.shutdown();
    }

    /**
     * Adds a remote repository to the list of repositories used for dependency resolution.
     *
     * @param repo The URL of the remote repository to be added.
     */
    public void addRepository(String repo) {
        if (repositories.stream().anyMatch(repository -> Objects.equals(repository.getUrl(), repo))) return;
        repositories.add(createRepository(repo));
    }

    /**
     * Creates a {@link RemoteRepository} instance using the given repository url.
     * <p>
     * This is a convenience method that automatically formats the repository identifier
     * using the provided url and delegates to
     * {@link #createRepository(String, String)} for actual repository construction.
     *
     * @param repo The url of the Maven repository.
     * @return A new {@link RemoteRepository} instance configured with the given url.
     */
    private RemoteRepository createRepository(String repo) {
        return createRepository("maven(url: %s)".formatted(repo), repo);
    }

    /**
     * Creates a {@link RemoteRepository} instance with the specified identifier and url.
     * <p>
     * This method sets up the repository with the default layout ({@code "default"})
     * and a default {@link RepositoryPolicy} for releases. Authentication and
     * additional configuration are currently not applied, but may be added in the future.
     *
     * @param id  The identifier of the repository (used internally by the repository system).
     * @param url The url of the Maven repository.
     * @return A new {@link RemoteRepository} instance configured with the given identifier and url.
     */
    private RemoteRepository createRepository(String id, String url) {
        // TODO: Load authentication from .credentials

        return new RemoteRepository.Builder(id, "default", url)
                .setReleasePolicy(new RepositoryPolicy())
                .build();
    }

    /**
     * Loads libraries for a specific addon based on the provided library coordinates.
     *
     * @param craftsNet   The {@link CraftsNet} instance that loads the libraries.
     * @param addonLoader The addon loader responsible for loading services.
     * @param services    The list with the currently registered services.
     * @param addon       The name of the addon for which libraries are being loaded.
     * @param libraries   The coordinates of the libraries to be loaded.
     * @return An array of URLs representing the loaded libraries.
     */
    public URL[] loadLibraries(CraftsNet craftsNet, AddonLoader addonLoader, Collection<RegisteredService> services,
                               String addon, String... libraries) {
        Logger logger = craftsNet.getLogger();
        logger.debug("Loading %s libraries for %s...", libraries.length, addon);
        List<Dependency> dependencies = new ArrayList<>();
        for (String library : libraries) {
            DefaultArtifact artifact = new DefaultArtifact(library);
            Dependency dependency = new Dependency(artifact, null);
            dependencies.add(dependency);
        }

        List<URL[]> urls = new ArrayList<>();
        dependencies.forEach(dependency -> urls.add(resolveDependency(logger, addonLoader, services, addon, dependency)));
        return urls.stream().flatMap(Arrays::stream).distinct().toArray(URL[]::new);
    }

    /**
     * Resolves a {@link Dependency dependency} into an array of {@link URL urls}.
     * The services from the META-INF are also handled during this resolution.
     *
     * @param logger      The logger of the current {@link CraftsNet} instance.
     * @param addonLoader The addon loader responsible for loading services.
     * @param services    The list with the currently registered services.
     * @param addon       The name of the addon for which libraries are being loaded.
     * @param dependency  The {@link Dependency dependency} to resolve.
     * @return The resolved array of {@link URL urls}.
     */
    private URL[] resolveDependency(Logger logger, AddonLoader addonLoader, Collection<RegisteredService> services,
                                    String addon, Dependency dependency) {
        DependencyResult result;
        try {
            CollectRequest collectRequest = new CollectRequest();

            collectRequest.setRootArtifact(dependency.getArtifact());

            collectRequest.setRoot(dependency);
            repositories.forEach(collectRequest::addRepository);

            DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, null);
            result = repository.resolveDependencies(session, dependencyRequest);
        } catch (DependencyResolutionException e) {
            throw new RuntimeException("Could not resolve dependency %s for %s!".formatted(dependency.toString(), addon), e);
        }

        List<URL> urls = new ArrayList<>();
        result.getArtifactResults().forEach(artifact -> {
            File file = artifact.getArtifact().getPath().toFile();

            try (JarFile jarFile = new JarFile(file, true, ZipFile.OPEN_READ, Runtime.version())) {
                urls.add(file.toURI().toURL());
                services.addAll(addonLoader.retrieveServices(jarFile));
            } catch (IOException e) {
                throw new RuntimeException("Could not process services of dependency %s for %s!".formatted(
                        dependency.toString(), addon
                ), e);
            }

            logger.debug("Loaded library %s for %s", file.getName(), addon);
        });

        return urls.toArray(URL[]::new);
    }

}