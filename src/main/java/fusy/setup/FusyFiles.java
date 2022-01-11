package fusy.setup;

import java.io.*;

public class FusyFiles {

    public InputStreamReader in(String path) {
        try {
            return new InputStreamReader(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputStreamWriter out(String path) {
        try {
            return new OutputStreamWriter(new FileOutputStream(path));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputStreamWriter out(String path, boolean append) {
        try {
            return new OutputStreamWriter(new FileOutputStream(path, append));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public OutputStreamWriter end(String path) {
        return out(path, true);
    }
}
