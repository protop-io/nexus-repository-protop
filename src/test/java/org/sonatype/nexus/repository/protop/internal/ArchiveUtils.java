
package org.sonatype.nexus.repository.protop.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

public class ArchiveUtils
{
  public static InputStream pack(final File tempFile, final File srcFile, final String archiveFilename) {
    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(tempFile)));
        InputStream in = new FileInputStream(srcFile)) {
        out.putArchiveEntry(out.createArchiveEntry(srcFile, archiveFilename));
        IOUtils.copy(in, out);
        out.closeArchiveEntry();
        out.finish();
      return new FileInputStream(tempFile);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
