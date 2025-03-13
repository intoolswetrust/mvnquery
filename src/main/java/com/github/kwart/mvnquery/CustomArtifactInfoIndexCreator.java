package com.github.kwart.mvnquery;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IndexerField;
import org.apache.maven.index.IndexerFieldVersion;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.creator.MinimalArtifactInfoIndexCreator;

public class CustomArtifactInfoIndexCreator extends MinimalArtifactInfoIndexCreator {

    public static final IndexerField FLD_LAST_MODIFIED = new IndexerField(
            MAVEN.LAST_MODIFIED,
            IndexerFieldVersion.V1,
            "m2",
            "Artifact last modified (not indexed, stored)",
            StoredField.TYPE);

    @Override
    public void updateDocument(ArtifactInfo ai, Document doc) {
        super.updateDocument(ai, doc);

        if (ai.getLastModified() > 0) {
            // Store lastModified as a number so we can query it
            doc.add(new LongPoint(FLD_LAST_MODIFIED.getKey(), ai.getLastModified()));
            // Also store it so we can retrieve it later
            doc.add(new StoredField(FLD_LAST_MODIFIED.getKey(), ai.getLastModified()));
        }
    }

	@Override
    public List<IndexerField> getIndexerFields() {
        List<IndexerField> res = new ArrayList<>(super.getIndexerFields());
		res.add(FLD_LAST_MODIFIED);
        return res;
    }
}
