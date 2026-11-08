package com.govener.attachment.adapter.out.validation;

import com.govener.attachment.application.AttachmentPorts;
import com.govener.shared.domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;

public final class FileContentValidator implements AttachmentPorts.ContentValidator {
  public void validate(byte[] bytes, String fileName, String mediaType, String purpose) {
    if (bytes == null || bytes.length == 0 || bytes.length > 5 * 1024 * 1024)
      throw RuleViolation.invalid("File must contain data and be at most 5 MB.");
    Checks.text(fileName, 180);
    if (fileName.contains("/")
        || fileName.contains("\\")
        || fileName.chars().anyMatch(c -> c < 32 || c == 127))
      throw RuleViolation.invalid("Invalid filename.");
    if (!Set.of("REQUEST_PHOTO", "PAYMENT_RECEIPT", "EXPENSE_DOCUMENT").contains(purpose))
      throw RuleViolation.invalid("Invalid file purpose.");
    String name = fileName.toLowerCase(Locale.ROOT);
    boolean valid = false;
    try {
      if ("image/png".equals(mediaType) && name.endsWith(".png")) {
        valid =
            bytes.length > 8
                && Arrays.equals(
                    Arrays.copyOf(bytes, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10})
                && validImage(bytes);
      } else if ("image/jpeg".equals(mediaType)
          && (name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
        valid =
            bytes.length > 3
                && (bytes[0] & 255) == 255
                && (bytes[1] & 255) == 216
                && (bytes[2] & 255) == 255
                && validImage(bytes);
      } else if ("image/webp".equals(mediaType)
          && name.endsWith(".webp")
          && purpose.equals("REQUEST_PHOTO")) {
        valid =
            bytes.length >= 20
                && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")
                && Set.of("VP8 ", "VP8L", "VP8X")
                    .contains(new String(bytes, 12, 4, StandardCharsets.US_ASCII));
      } else if ("application/pdf".equals(mediaType)
          && name.endsWith(".pdf")
          && !purpose.equals("REQUEST_PHOTO")) {
        valid =
            bytes.length > 8
                && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")
                && new String(
                        bytes,
                        Math.max(0, bytes.length - 1024),
                        Math.min(1024, bytes.length),
                        StandardCharsets.ISO_8859_1)
                    .contains("%%EOF");
      }
    } catch (IOException failure) {
      valid = false;
    }
    if (!valid)
      throw RuleViolation.invalid(
          "File content, extension, media type or purpose is not supported.");
  }

  private boolean validImage(byte[] bytes) throws IOException {
    try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(stream);
      if (!readers.hasNext()) return false;
      var reader = readers.next();
      try {
        reader.setInput(stream);
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width <= 0
            || height <= 0
            || width > 10000
            || height > 10000
            || (long) width * height > 40_000_000) return false;
        return reader.read(0) != null;
      } finally {
        reader.dispose();
      }
    }
  }
}
