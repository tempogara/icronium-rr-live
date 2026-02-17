package it.icron.icronium.live;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class FileDownloader {

    /**
     * "Locale": mantiene il comportamento precedente.
     * Se qui in realtà passi un path locale, lo copia in WORK_DIR.
     * (Se in futuro vuoi gestire IP reali via rete, lo faremo qui.)
     */
	public static Path download(String src) throws IOException {

	    Path source = Paths.get(src);
	    String fileName = source.getFileName().toString();

	    Path target = MainApp.WORK_DIR.resolve(fileName);

	    // copia e sovrascrive mantenendo nome fisso
	    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

	    return target;
	}

    /**
     * Remoto HTTP/HTTPS:
     * - salva in WORK_DIR
     * - nome file = nome originale dell'URL
     * - scarica su .tmp
     * - sostituisce SOLO se la size del nuovo > size del precedente
     * - se il nuovo è più piccolo/uguale, scarta il tmp e ritorna il vecchio
     */
    public static Path downloadHttp(String urlStr) throws Exception {

        URL url = new URL(urlStr);

        // nome originale del file preso dall'URL
        String fileName = Path.of(url.getPath()).getFileName().toString();

        Path target = MainApp.WORK_DIR.resolve(fileName);
        Path temp = MainApp.WORK_DIR.resolve(fileName + ".tmp");

        // download in file temporaneo
        try (InputStream in = url.openStream()) {
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
        }

        long newSize = Files.size(temp);

        if (Files.exists(target)) {
            long oldSize = Files.size(target);

            // rete instabile / file troncato → scarta
            if (newSize <= oldSize) {
                Files.deleteIfExists(temp);
                return target;
            }
        }

        // replace "atomico" quando possibile
        try {
            Files.move(temp, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            // fallback se ATOMIC_MOVE non è supportato sul filesystem
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }

        return target;
    }

    /**
     * Ripristinato: conta righe del file.
     */
    public static long countLines(Path p) throws IOException {
        try (Stream<String> s = Files.lines(p)) {
            return s.count();
        }
    }
}
