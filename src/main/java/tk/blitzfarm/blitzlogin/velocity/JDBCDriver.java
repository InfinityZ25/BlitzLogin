/*
 * This file is part of BlitzLogin, licensed under the MIT License.
 *
 *  Copyright (c) 2021 XXMA16
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package tk.blitzfarm.blitzlogin.velocity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;

public abstract class JDBCDriver {
    private static final File driver = new File("plugins/blitzlogin/lib/jdbc-driver.jar");

    public static void inject(BlitzLogin plugin) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            if (!driver.exists()) download();
            plugin.getProxy().getPluginManager().addToClasspath(plugin, Paths.get(driver.getAbsolutePath()));
        }
    }

    private static void download() {
        driver.getParentFile().mkdir();
        File temp = new File("temp_blitzlogin/");
        temp.mkdir();
        Path zip = Paths.get(temp.getAbsolutePath(), "jdbc.zip");
        try {
            try (InputStream in = new URL("https://dev.mysql.com/get/Downloads/Connector-J/mysql-connector-java-8.0.26.zip").openStream()) {
                Files.copy(in, zip, StandardCopyOption.REPLACE_EXISTING);
            }
            try (FileSystem zipFileSystem = FileSystems.newFileSystem(zip, null)) {
                Files.copy(zipFileSystem.getPath("mysql-connector-java-8.0.26/mysql-connector-java-8.0.26.jar"), Paths.get(driver.getParentFile().getAbsolutePath(), "jdbc-driver.jar"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to download JDBC!");
        }
        try {
            Files.delete(Paths.get(temp.getAbsolutePath(), "jdbc.zip"));
            Files.delete(Paths.get(temp.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
