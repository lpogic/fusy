package fusy.setup;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FusyFiles {

    public InputStreamReader in(String path) {
        try {
            return new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputStreamWriter out(String path) {
        try {
            return new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputStreamWriter out(String path, boolean append) {
        try {
            return new OutputStreamWriter(new FileOutputStream(path, append), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputStreamWriter end(String path) {
        return out(path, true);
    }
}
