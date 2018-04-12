package org.molgenis.vibe.ontology_processing;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;

import org.apache.jena.util.iterator.ExtendedIterator;
import org.molgenis.vibe.formats.Phenotype;
import org.molgenis.vibe.formats.PhenotypeNetwork;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class MaxDistanceRetriever2 extends PhenotypesRetriever {
    /**
     * The maximum distance.
     */
    private int maxDistance;

    public MaxDistanceRetriever2(OntModel model, Collection<Phenotype> phenotypes, int maxDistance) {
        super(model, phenotypes);
        this.maxDistance = requireNonNull(maxDistance);
    }

    @Override
    public void run() {
        for(Phenotype phenotype:getInputPhenotypes()) {
            PhenotypeNetwork network = new PhenotypeNetwork(phenotype);

            Set<OntClass> startOC = new HashSet<>();
            startOC.add(retrievePhenotypeFromModel(phenotype));
            traverse(startOC, network, 0);

            getPhenotypeNetworkCollection().add(network);
        }
    }

    /**
     * Traverses the {@link OntModel}.
     * @param phenotypeOCsToDigest current item being traversed
     * @param network stores the {@link Phenotype}{@code s} based on traversal
     * @param distance current distance from the {@code network source} (see {@link PhenotypeNetwork#getSource()})
     */
    private void traverse(Set<OntClass> phenotypeOCsToDigest, PhenotypeNetwork network, int distance) {
        Set<OntClass> nextPhenotypeOCsToDigest = new HashSet<>();

        for(OntClass phenotypeOC : phenotypeOCsToDigest) {
            if(skippableUri(phenotypeOC)) {
                continue;
            }

            addPhenotypeToNetwork(phenotypeOC, network, distance);

            if(distance < maxDistance) {
                // Goes through the parents.
                for (ExtendedIterator<OntClass> it = phenotypeOC.listSuperClasses(true); it.hasNext(); ) {
                    nextPhenotypeOCsToDigest.add(it.next());
                }

                // Goes through the children.
                for (ExtendedIterator<OntClass> it = phenotypeOC.listSubClasses(true); it.hasNext(); ) {
                    nextPhenotypeOCsToDigest.add(it.next());
                }
            }
        }

        if(nextPhenotypeOCsToDigest.size() > 0) {
            traverse(nextPhenotypeOCsToDigest, network, distance + 1);
        }
    }
}
