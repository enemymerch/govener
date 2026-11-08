package com.govener.attachment.adapter.out.mongo;

import com.govener.attachment.application.AttachmentPorts;
import com.govener.shared.domain.RuleViolation;
import java.io.*;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

public final class GridFsBlobAdapter implements AttachmentPorts.Blobs {
  private final GridFsTemplate grid;

  public GridFsBlobAdapter(GridFsTemplate grid) {
    this.grid = grid;
  }

  public String store(byte[] bytes, String name, String mediaType) {
    return grid.store(new ByteArrayInputStream(bytes), name, mediaType).toHexString();
  }

  public byte[] read(String id) {
    try {
      var file = grid.findOne(Query.query(Criteria.where("_id").is(new ObjectId(id))));
      if (file == null) throw RuleViolation.missing();
      try (var stream = grid.getResource(file).getInputStream()) {
        return stream.readAllBytes();
      }
    } catch (IOException failure) {
      throw new IllegalStateException("Stored file could not be read.", failure);
    }
  }

  public void delete(String id) {
    grid.delete(Query.query(Criteria.where("_id").is(new ObjectId(id))));
  }
}
