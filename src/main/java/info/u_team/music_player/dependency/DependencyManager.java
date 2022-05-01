package info.u_team.music_player.dependency;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import info.u_team.music_player.MusicPlayerMod;
import info.u_team.music_player.dependency.classloader.DependencyClassLoader;
import info.u_team.music_player.dependency.classloader.JarLookupURLStreamHandlerProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class DependencyManager {
	
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker MARKER_LOAD = MarkerManager.getMarker("Load");
	private static final Marker MARKER_ADD = MarkerManager.getMarker("Add");
	
	private static final String FILE_ENDING = ".jar.packed";
	
	public static final DependencyClassLoader MUSICPLAYER_CLASSLOADER = new DependencyClassLoader();
	
	public static void load() {
		LOGGER.info(MARKER_LOAD, "Load dependencies");
		
		final String devPath = System.getProperty("musicplayer.dev");
		if (devPath != null) {
			findJarFilesInDev(Paths.get(devPath, "musicplayer-lavaplayer/build/libs"), path -> addToMusicPlayerDependencies(pathToUrl().apply(path)));
			findJarFilesInDev(Paths.get(devPath, "musicplayer-lavaplayer/build/dependencies"), path -> addToMusicPlayerDependencies(pathToUrl().apply(path)));
		} else {
			findJarFilesInJar("dependencies", path -> addToMusicPlayerDependencies(createInternalURL(path)));
		}
		
		LOGGER.info(MARKER_LOAD, "Finished loading dependencies");
	}
	
	private static Function<Path, URL> pathToUrl() {
		return path -> {
			try {
				return path.toUri().toURL();
			} catch (final MalformedURLException ex) {
				throw new RuntimeException(ex);
			}
		};
	}
	
	private static void findJarFilesInDev(Path path, Consumer<Path> consumer) {
		try (final Stream<Path> stream = Files.walk(path)) {
			stream.filter(file -> file.toString().endsWith(FILE_ENDING)).forEach(consumer);
		} catch (final IOException ex) {
			LOGGER.error(MARKER_LOAD, "When searching for jar files in dev an exception occured.", ex);
		}
	}
	
	private static void findJarFilesInJar(String folder, Consumer<Path> consumer) {
		final ModContainer container = FabricLoader.getInstance().getModContainer(MusicPlayerMod.MODID).orElseThrow(IllegalStateException::new);
		
		try (final Stream<Path> stream = Files.walk(container.findPath(folder).orElseThrow(IllegalStateException::new))) {
			stream.filter(file -> file.toString().endsWith(FILE_ENDING)).forEach(consumer);
		} catch (final IOException | IllegalStateException ex) {
			LOGGER.error(MARKER_LOAD, "When searching for jar files in jar an exception occured.", ex);
		}
	}
	
	private static URL createInternalURL(Path path) {
		final String url = JarLookupURLStreamHandlerProvider.PROTOCOL + "://" + MusicPlayerMod.MODID + "/" + path;
		LOGGER.debug(MARKER_LOAD, "Create mod jar url ({}) from path ({}).", url, path);
		try {
			return new URL(url);
		} catch (final MalformedURLException ex) {
			LOGGER.error(MARKER_LOAD, "Could not create url from internal path.", ex);
		}
		return null;
	}
	
	// Add to different classloader
	
	private static void addToMusicPlayerDependencies(URL url) {
		MUSICPLAYER_CLASSLOADER.addURL(url);
		LOGGER.debug(MARKER_ADD, "Added new jar file ({}) to the musicplayer dependency classloader.", url);
	}
}
