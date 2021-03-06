package org.molgenis.vibe.formats;

import org.molgenis.vibe.exceptions.InvalidStringFormatException;

import java.net.URI;

/**
 * Defines a phenotype.
 */
public class Phenotype extends BiologicalEntity {
    private static final String ID_PREFIX = "hp:";
    private static final String ID_REGEX = "^(hp|HP):([0-9]{7})$";
    private static final int REGEX_ID_GROUP = 2;
    private static final String URI_PREFIX = "http://purl.obolibrary.org/obo/HP_";

    @Override
    protected String getIdPrefix() {
        return ID_PREFIX;
    }

    @Override
    protected String getIdRegex() {
        return ID_REGEX;
    }

    @Override
    protected int getRegexIdGroup() {
        return REGEX_ID_GROUP;
    }

    @Override
    protected String getUriPrefix() {
        return URI_PREFIX;
    }

    public Phenotype(String id) {
        super(id);
    }

    public Phenotype(URI uri) {
        super(uri);
    }

    public Phenotype(String id, String name, URI uri) throws InvalidStringFormatException {
        super(id, name, uri);
    }

    @Override
    public String toString() {
        return "Phenotype{} " + super.toString();
    }
}
