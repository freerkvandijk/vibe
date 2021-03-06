package org.molgenis.vibe.io.output;


import org.molgenis.vibe.formats.GeneDiseaseCollection;
import org.molgenis.vibe.query_output_digestion.prioritization.Prioritizer;

import java.nio.file.Path;

public enum FileOutputWriterFactory {
    SIMPLE {
        @Override
        public FileOutputWriter create(Path path, GeneDiseaseCollection geneDiseaseCollection, Prioritizer prioritizer) {
            return new OrderedGenesOutputWriter(path, prioritizer.getPriority(), ValuesSeparator.COMMA);
        }
    },
    REGULAR {
        @Override
        public FileOutputWriter create(Path path, GeneDiseaseCollection geneDiseaseCollection, Prioritizer prioritizer) {
            return new ResultsPerGeneSeparatedValuesFileOutputWriter(path, geneDiseaseCollection, prioritizer.getPriority(),
                    ValuesSeparator.TAB, ValuesSeparator.VERTICAL_LINE, ValuesSeparator.COLON, ValuesSeparator.COMMA);
        }
    };

    public abstract FileOutputWriter create(Path path, GeneDiseaseCollection geneDiseaseCollection, Prioritizer prioritizer);
}
